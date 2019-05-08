import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * create by Eric
 * 2019/01/05
 */
public class NotEmptyProvider extends XMLLanguageDriver implements LanguageDriver {

    Logger logger = LoggerFactory.getLogger(NotEmptyProvider.class);

    @Override
    public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        return super.createParameterHandler(mappedStatement, parameterObject, boundSql);
    }

    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        SqlSource sqlSource;
        if (script.startsWith("<script>") || script.toLowerCase().lastIndexOf("where") < 0) {
            sqlSource = super.createSqlSource(configuration, script, parameterType);
        } else {
            String newScript = createNotEmptyScript(script);
            sqlSource = super.createSqlSource(configuration, newScript, parameterType);
        }
        //sqlSource.getBoundSql().getSql();
        return sqlSource;
    }

    private String createNotEmptyScript(String script) {
        Long startTime = System.currentTimeMillis();
        logger.info("=====================生成script==========================" + startTime);
        StringBuffer newScript = new StringBuffer();
        newScript.append("<script>\n");
        //script = script.toLowerCase();
        //script = script.toLowerCase();

        newScript.append(script.substring(0,script.toLowerCase().lastIndexOf("where") + 5));
        newScript.append(" 1 = 1 ");
        String paramSql = script.substring(script.toLowerCase().lastIndexOf("where") + 5);
        boolean hasFirst = true;
        while (paramSql.indexOf("#{") >= 0 || paramSql.indexOf("${") >= 0) {
            newScript.append(" ");
            int paramIndex = paramSql.indexOf("#{");
            int dorIndex = paramSql.indexOf("${");
            if(dorIndex >= 0 && dorIndex < paramIndex) {
                paramIndex = dorIndex;
            }
            int endIndex = paramSql.indexOf("}") + 1;
            String tempParam = paramSql.substring(0, endIndex);

            int startIndex = -1;
/*            int oneStartInd = paramSql.lastIndexOf("(");
            int oneEndInd = paramSql.lastIndexOf(")");
            if(paramIndex > oneStartInd && paramIndex < oneEndInd) {
                startIndex = oneStartInd;
            }*/
            if(startIndex >= 0) {
                startIndex++;
            }
            if(startIndex < 0) {
                startIndex = tempParam.toLowerCase().lastIndexOf(" and ");
            }
            if(startIndex < 0) {
                startIndex = tempParam.toLowerCase().lastIndexOf(" or ");
            }
            if(startIndex < 0) {
                startIndex = 0;
            }
            //where后面并未直接跟#{}参数而是普通查询参数，因为前面有个1=1所以需要拼接一个and
            if(paramIndex > 0 && hasFirst && startIndex > 0) {
                newScript.append(" and ");
                hasFirst = false;
            }
            newScript.append(paramSql.substring(0, startIndex));

            String paramName = paramSql.substring(paramIndex + 2, endIndex-1);
            if(paramName.indexOf(",") > 0) {
                paramName = paramName.substring(0, paramName.indexOf(","));
            }
            newScript.append("\n<if test='" + paramName + "!=null and " + paramName + "!=\"\"'>\n");
            tempParam = paramSql.substring(startIndex, endIndex);
            if(tempParam.toLowerCase().indexOf("like") > 0) {
                endIndex = paramSql.indexOf("%||'");
                if(endIndex <= 0) {
                    endIndex = paramSql.indexOf("%'");
                    if(paramIndex > endIndex) {
                        endIndex = paramSql.indexOf("%'", endIndex+1);
                        endIndex += 3;
                    } else {
                        endIndex += 2;
                    }
                } else {
                    endIndex += 4;
                }
                tempParam = paramSql.substring(startIndex, endIndex);
            }
            //若这是where后第一个查询参数，那么需要拼接一个and
            if(startIndex == 0 && hasFirst) {
                newScript.append(" and ");
                hasFirst = false;
            }
            newScript.append(tempParam);
            newScript.append(" ");
            newScript.append("\n</if>\n");
            paramSql = paramSql.substring(endIndex);
        }
        newScript.append(paramSql);
        newScript.append("\n</script>");

        Long endTime = System.currentTimeMillis();
        logger.info("=====================script结束==========================" + endTime);
        logger.info("=====================耗时================================" + (endTime - startTime));
        return newScript.toString();
    }

    public static void main(String[] args) {
        NotEmptyProvider provider = new NotEmptyProvider();
        String script = "SELECT k.`id`,k.`name`,k.`address`,k.`nature`,k.`people_num`,k.`is_deal`,k.`level`,k.`intention`,k.`is_group`,k.`description`,k.`remark`,k.`come_source`,k.`operator_id`, o.name as operator_name," +
                " ap.`area_name` AS province, ac.`area_name` AS city, ad.`area_name` AS district, kf.next_time, " +
                " IF(kf.next_time IS NULL, 0, 1) nonext  " +
                " FROM c_kindergarten k " +
                " LEFT JOIN dt_area ap ON ap.`id` = k.`province`  " +
                " LEFT JOIN dt_area ac ON ac.`id` = k.`city`  " +
                " LEFT JOIN dt_area ad ON ad.`id` = k.`district`  " +
                " left join s_operator o on o.id = k.operator_id " +
                " LEFT JOIN c_kindergarten_follow kf ON kf.k_id = k.id AND kf.id = (SELECT MAX(kf1.id) FROM c_kindergarten_follow kf1 WHERE kf1.`k_id` = kf.k_id)  " +
                " LEFT JOIN (c_customer_kindergarten ck, c_customer c) ON (c.`id` = ck.`customer_id` AND ck.`kindergarten_id` = k.`id`) " +
                " WHERE k.operator_id = #{operatorId} " +
                " and k.province = #{province} " +
                " and k.city = #{city} " +
                " and k.district = #{district} " +
                " and k.intention = #{intention}" +
                " and k.name like concat('%',#{name},'%') " +
                " and o.phone like concat('%',#{operatorPhone},'%') " +
                " and c.phone like concat('%',#{linkPhone},'%') " +
                " and k.intention = #{intention} " +
                " and k.nature = #{nature} " +
                " and k.level = #{level} " +
                " and k.is_group = #{isGroup} " +
                " and k.is_deal = #{isDeal} " +
                " GROUP BY `id` ORDER BY nonext DESC, kf.next_time ASC, k.id DESC";
        System.out.println(provider.createNotEmptyScript(script));
    }

}
