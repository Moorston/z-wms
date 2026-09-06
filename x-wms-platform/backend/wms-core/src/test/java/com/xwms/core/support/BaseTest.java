package com.xwms.core.support;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

/**
 * 单元测试基类
 * 提供通用的Mock初始化和测试工具方法
 */
public abstract class BaseTest {

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 等待异步操作完成
     */
    protected void sleep(long millis)
    {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e)
    {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 断言异常消息包含指定内容
     */
    protected void assertExceptionMessage(Runnable runnable, String keyword)
    {
        try {
            runnable.run();
            throw new AssertionError("Expected exception not thrown");
        } catch (Exception e)
    {
            if (!e.getMessage().contains(keyword))
    {
                throw new AssertionError("Exception message doesn't contain '" + keyword +
                        "', actual: " + e.getMessage());
            }
        }
    }
}
