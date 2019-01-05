# NotEmptyProvider

#### 介绍
Mybatis自动对查询参数添加为空判断，在查询时过滤掉参数为空的查询条件


#### 使用说明

1. 在程序启动时对sql拼接<script>内容
2. 在需要做判断的@Select注解下添加@Lang(NotEmptyProvider.class)即可
