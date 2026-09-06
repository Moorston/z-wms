"""WMS AI Platform - 对话会话管理"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from modules.rag.chat.session import ChatSessionManager

__all__ = ["ChatSessionManager"]
