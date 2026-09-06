"""WMS AI Platform - 知识库管理"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from modules.rag.knowledge.manager import KnowledgeManager

__all__ = ["KnowledgeManager"]
