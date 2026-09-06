"""
WMS AI Platform - 知识图谱存储模块
MySQL 持久化 + 内存邻接表（查询 <1ms）
"""
import os
import sys
from typing import Dict, List, Tuple, Optional

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from loguru import logger
from common.data_client import mysql_client
from modules.rag.kg.extraction import Triple


class KGStore:
    """
    知识图谱存储：MySQL 持久化 + 内存邻接表
    邻接表结构：Dict[str, List[Tuple[predicate, object, confidence]]]
    """

    def __init__(self):
        # 内存邻接表：subject -> [(predicate, object, confidence)]
        self._adjacency: Dict[str, List[Tuple[str, str, float]]] = {}
        # 反向索引：object -> [(predicate, subject, confidence)]
        self._reverse: Dict[str, List[Tuple[str, str, float]]] = {}
        # 节点类型索引：entity_name -> entity_type
        self._node_types: Dict[str, str] = {}

    def load_from_mysql(self) -> int:
        """启动时从 MySQL 重建邻接表，返回加载三元组数"""
        try:
            rows = mysql_client.query(
                "SELECT subject, predicate, object, confidence, subject_type, object_type "
                "FROM kg_triple"
            )
            count = 0
            for row in rows:
                subject = row.get("subject", "")
                predicate = row.get("predicate", "")
                obj = row.get("object", "")
                confidence = float(row.get("confidence", 1.0))
                subj_type = row.get("subject_type", "")
                obj_type = row.get("object_type", "")

                # 正向边
                self._adjacency.setdefault(subject, [])
                self._adjacency[subject].append((predicate, obj, confidence))
                # 反向边
                self._reverse.setdefault(obj, [])
                self._reverse[obj].append((predicate, subject, confidence))
                # 节点类型
                if subj_type:
                    self._node_types[subject] = subj_type
                if obj_type:
                    self._node_types[obj] = obj_type
                count += 1

            logger.info(f"KG 邻接表重建完成: {count} 条三元组, {len(self._adjacency)} 个节点")
            return count
        except Exception as e:
            logger.warning(f"KG 从 MySQL 加载失败: {e}")
            return 0

    def save_triples(self, triples: List[Triple]) -> int:
        """
        写入 MySQL + 更新内存邻接表
        返回成功写入数
        """
        if not triples:
            return 0

        count = 0
        for t in triples:
            # 写入 MySQL
            try:
                mysql_client.execute(
                    "INSERT INTO kg_triple (subject, subject_type, predicate, object, object_type, confidence, doc_id) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s)",
                    (t.subject, t.subject_type, t.predicate, t.object, t.object_type,
                     t.confidence, t.doc_id)
                )
                count += 1
            except Exception as e:
                logger.warning(f"KG 三元组写入 MySQL 失败: {e}")

            # 更新内存邻接表（无论 MySQL 是否成功都更新内存，保证查询可用）
            self._adjacency.setdefault(t.subject, [])
            self._adjacency[t.subject].append((t.predicate, t.object, t.confidence))
            self._reverse.setdefault(t.object, [])
            self._reverse[t.object].append((t.predicate, t.subject, t.confidence))
            if t.subject_type:
                self._node_types[t.subject] = t.subject_type
            if t.object_type:
                self._node_types[t.object] = t.object_type

        logger.info(f"KG 三元组写入完成: {count}/{len(triples)} 条")
        return count

    def delete_by_doc(self, doc_id: str) -> int:
        """
        删除文档关联的三元组（MySQL + 内存）
        返回删除数
        """
        deleted = 0
        try:
            rows = mysql_client.query(
                "SELECT id, subject, predicate, object FROM kg_triple WHERE doc_id = %s",
                (doc_id,)
            )
            if rows:
                ids = [row["id"] for row in rows]
                id_list = ",".join(str(i) for i in ids)
                mysql_client.execute(
                    f"DELETE FROM kg_triple WHERE id IN ({id_list})",
                )
                deleted = len(ids)

                # 从内存中移除
                for row in rows:
                    subj = row.get("subject", "")
                    pred = row.get("predicate", "")
                    obj = row.get("object", "")
                    # 正向边
                    if subj in self._adjacency:
                        self._adjacency[subj] = [
                            (p, o, c) for p, o, c in self._adjacency[subj]
                            if not (p == pred and o == obj)
                        ]
                        if not self._adjacency[subj]:
                            del self._adjacency[subj]
                    # 反向边
                    if obj in self._reverse:
                        self._reverse[obj] = [
                            (p, s, c) for p, s, c in self._reverse[obj]
                            if not (p == pred and s == subj)
                        ]
                        if not self._reverse[obj]:
                            del self._reverse[obj]
        except Exception as e:
            logger.warning(f"KG 删除文档三元组失败: doc_id={doc_id}, error={e}")

        logger.info(f"KG 删除完成: doc_id={doc_id}, deleted={deleted}")
        return deleted

    def get_neighbors(self, node: str, max_hops: int = 2) -> List[Dict]:
        """
        BFS 1-2 跳展开，返回路径列表
        路径格式：[{"path": [(entity, predicate, entity, ...)], "hop_count": N, "confidence": C}]
        """
        results = []
        visited = {node}
        current_nodes = [node]

        for hop in range(1, max_hops + 1):
            next_nodes = []
            for current in current_nodes:
                neighbors = self._adjacency.get(current, [])
                # 也检查反向
                reverse_neighbors = self._reverse.get(current, [])

                for pred, target, conf in neighbors:
                    if target not in visited:
                        visited.add(target)
                        results.append({
                            "path": [(current, pred, target)],
                            "hop_count": hop,
                            "confidence": conf,
                        })
                        next_nodes.append(target)

                # 反向遍历（用于补充上下文）
                for pred, source, conf in reverse_neighbors:
                    if source not in visited:
                        visited.add(source)
                        results.append({
                            "path": [(source, pred, current)],
                            "hop_count": hop,
                            "confidence": conf * 0.8,  # 反向边置信度衰减
                        })
                        next_nodes.append(source)

            # 去重下一层节点
            next_nodes = list(set(next_nodes))
            if not next_nodes:
                break
            current_nodes = next_nodes

        return results

    def has_node(self, node: str) -> bool:
        """检查节点是否存在"""
        return node in self._adjacency or node in self._reverse

    def get_node_type(self, node: str) -> str:
        """获取节点类型"""
        return self._node_types.get(node, "")

    def stats(self) -> Dict:
        """返回图谱统计"""
        return {
            "node_count": len(set(list(self._adjacency.keys()) + list(self._reverse.keys()))),
            "edge_count": sum(len(v) for v in self._adjacency.values()),
            "adjacency_size": len(self._adjacency),
            "reverse_size": len(self._reverse),
            "node_types": len(self._node_types),
        }
