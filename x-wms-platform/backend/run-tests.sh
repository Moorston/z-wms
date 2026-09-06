#!/bin/bash
export JAVA_HOME="E:/SDKMan/candidates/java/21.0.11-tem"
cd "C:/Users/freebytes/Doubao/chats/2026-08-16/new-chat-1/x-wms-platform/backend"
E:/Maven/apache-maven-3.9.6/bin/mvn test \
  -pl wms-common,wms-integration,wms-core,wms-analytics \
  -Dtest="SentinelConfigTest,FallbackHandlerTest,MybatisPlusConfigTest,ExpressGetServiceFallbackTest,OutboundOrderServiceFallbackTest,BillingMigrationTest" \
  -DfailIfNoTests=false \
  -Dcheckstyle.skip=true \
  2>&1
