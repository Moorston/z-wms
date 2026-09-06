"""WMS AI Platform - LLM 答案生成器"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from modules.rag.generator.llm_generator import LLMGenerator

__all__ = ["LLMGenerator"]
