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
        script = script.toLowerCase();
        newScript.append(script.substring(0,script.lastIndexOf("where") + 5));
        newScript.append(" 1 = 1");
        String paramSql = script.substring(script.lastIndexOf("where") + 5);
        boolean hasFirst = true;
        while (paramSql.indexOf("#{") >= 0) {
            newScript.append(" ");
            int paramIndex = paramSql.indexOf("#{");
            if(paramIndex > 0 && hasFirst) {
                newScript.append(" and ");
                hasFirst = false;
            }
            int endIndex = paramSql.indexOf("}") + 1;
            String tempParam = paramSql.substring(0, endIndex);
            int startIndex = paramSql.lastIndexOf("(");
            if(startIndex >= 0) {
                startIndex++;
            }
            if(startIndex < 0) {
                startIndex = tempParam.lastIndexOf("and");
            }
            if(startIndex < 0) {
                startIndex = 0;
            }
            newScript.append(paramSql.substring(0, startIndex));
            String paramName = paramSql.substring(paramIndex + 2, endIndex-1);
            newScript.append("\n<if test='" + paramName + "!=null'>\n");
            tempParam = paramSql.substring(startIndex, endIndex);
            if(tempParam.indexOf("like") > 0) {
                endIndex = paramSql.indexOf("%'") + 2;
                tempParam = paramSql.substring(startIndex, endIndex);
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
        String script = "SELECT o.`name`,a.`area_name`,o.`phone`,COUNT(k.`id`) AS cus_num,o.`id` FROM c_kindergarten k, s_operator o LEFT JOIN s_area a ON o.`area_id` = a.`id` " +
                "WHERE o.`id` = k.`operator_id` " +
                "and o.id = #{id} and o.phone = #{phone} " +
                "GROUP BY k.`operator_id`";
        System.out.println(provider.createNotEmptyScript(script));
    }

}
