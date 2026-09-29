package com.ljr.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dashscope")
class TravelManusTest {

    @Resource
    private ObjectProvider<TravelManus> travelManusProvider;

    @Test
    void travelManus_shouldPlanAndPossiblyGeneratePdf() {
        Assumptions.assumeTrue(
                System.getenv("DASHSCOP_API_KEY") != null
                        && !System.getenv("DASHSCOP_API_KEY").isBlank(),
                "跳过：未设置 DASHSCOP_API_KEY"
        );

        TravelManus manus = travelManusProvider.getObject();
        assertNotNull(manus);

        String result = manus.run(
                "请帮我规划一个极简的京都半日游（只含清水寺），"
                        + "把行程写成简短文本并生成 PDF（文件名 kyoto-halfday.pdf），"
                        + "完成后结束任务。不要做过多联网搜索。");

        assertNotNull(result);
        assertFalse(result.isBlank());
        System.out.println("TravelManus 结果:\n" + result);
        assertTrue(
                result.contains("Step")
                        || result.contains("工具")
                        || result.contains("PDF")
                        || result.contains("完成")
                        || result.contains("终止"),
                "结果应体现多步执行过程"
        );
    }
}
