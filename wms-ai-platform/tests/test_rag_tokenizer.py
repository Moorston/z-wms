"""
测试：RAG 中文分词（jieba + WMS 领域词表）
"""
import pytest
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from modules.rag.retriever.keyword import tokenize, _ensure_jieba_loaded


class TestTokenize:
    """中文分词器测试"""

    def setup_method(self):
        _ensure_jieba_loaded()

    def test_chinese_single_char_split(self):
        """传统正则会把中文拆成单字，jieba + 领域词表应保留词"""
        # "库存预占" 在领域词表中作为整体词，不应被拆成单字
        tokens = tokenize("库存预占")
        multi_char = [t for t in tokens if len(t) > 1]
        assert len(multi_char) > 0, f"应有至少一个多字词，实际 tokens={tokens}"
        # 验证非领域词仍会被正确切分
        tokens2 = tokenize("仓库管理系统")
        multi_char2 = [t for t in tokens2 if len(t) > 1]
        assert len(multi_char2) > 0, f"'仓库管理系统' 应产生多字词: {tokens2}"

    def test_wms_domain_words(self):
        """WMS 领域词表词应正确切分"""
        for word in ["波次", "库位", "拣货", "预占", "盘点", "库区"]:
            tokens = tokenize(word)
            assert word in tokens, f"'{word}' 未被正确切分，tokens={tokens}"

    def test_mixed_text(self):
        """混合中英文数字应正确切分"""
        tokens = tokenize("WMS系统使用PDA扫描条码12345")
        assert "wms" in tokens, f"WMS 未切分: {tokens}"
        assert "pda" in tokens, f"PDA 未切分: {tokens}"
        assert "12345" in tokens, f"数字未切分: {tokens}"

    def test_punctuation_filtered(self):
        """标点符号应被过滤（不保留空串）"""
        tokens = tokenize("，。！？、；：（）【】《》")
        assert all(t.strip() for t in tokens), f"不应有空字符串: {tokens}"

    def test_empty_string(self):
        """空字符串应返回空列表"""
        tokens = tokenize("")
        assert tokens == []

    def test_word_dict_loaded(self):
        """确认领域词表已加载"""
        import jieba
        # 检查词表中的特殊组合词
        tokens = tokenize("库存预占")
        assert "库存预占" in tokens, f"领域组合词'库存预占'应作为一个整体: {tokens}"

    def test_bm25_integration(self):
        """jieba 分词结果可用于 BM25"""
        tokens = tokenize("库存预占和库存扣减的区别")
        assert len(tokens) >= 3, f"应产生至少 3 个有效 token: {tokens}"
