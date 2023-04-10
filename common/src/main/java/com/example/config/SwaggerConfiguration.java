package com.example.config;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.*;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

//以后把公共配置都配置到com.example.config下
@Component
@Data
@EnableOpenApi
public class SwaggerConfiguration {

    /**
     * 对C端用户的接口文档
     * @return
     */
    @Bean
    private Docket webApiDoc(){
        //OAS_30采用3.0的配置
        return new Docket(DocumentationType.OAS_30)
                //分组，有对用户的，有对企业的
                .groupName("用户端接口文档")
                .pathMapping("/")
                // 定义是否开启swagger，false为关闭，可以通过变量控制，线上关闭
                //让线上不能访问接口文档
                .enable(true)
                //配置api文档元信息
                .apiInfo(apiInfo())
                // 选择哪些接口作为swagger的doc发布
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example"))
                //正则匹配请求路径，并分配至当前分组
                //api下的全部都要加入接口文档
                .paths(PathSelectors.ant("/api/**"))
                .build()
                .globalRequestParameters(getGlobalRequestParameters())
                .globalResponses(HttpMethod.GET, getGlobalResponseMessage())
                .globalResponses(HttpMethod.POST, getGlobalResponseMessage());
    }

    /**
     * 对管理端用户的接口文档
     * @return
     */
    @Bean
    private Docket adminApiDoc(){
        //OAS_30采用3.0的配置
        return new Docket(DocumentationType.OAS_30)
                //分组，有对用户的，有对企业的
                .groupName("管理端接口文档")
                .pathMapping("/")
                // 定义是否开启swagger，false为关闭，可以通过变量控制，线上关闭
                //让线上不能访问接口文档
                .enable(true)
                //配置api文档元信息
                .apiInfo(apiInfo())
                // 选择哪些接口作为swagger的doc发布
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example"))
                //正则匹配请求路径，并分配至当前分组
                //api下的全部都要加入接口文档
                .paths(PathSelectors.ant("/api/**"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("1024电商平台")
                .description("微服务接口文档")
                //练习哪个人
                .contact(new Contact("qiangw", "", "321516900@qq.com"))
                .version("v1.0")
                .build();
    }

    /**
     * 生成全局通用参数, 支持配置多个响应参数
     * @return
     */
    private List<RequestParameter> getGlobalRequestParameters() {
        List<RequestParameter> parameters = new ArrayList<>();
        parameters.add(new RequestParameterBuilder()
                .name("token")
                .description("登录令牌")
                .in(ParameterType.HEADER)
                .query(q -> q.model(m -> m.scalarModel(ScalarType.STRING)))
                .required(false)
                .build());

//        parameters.add(new RequestParameterBuilder()
//                .name("version")
//                .description("版本号")
//                .required(true)
//                .in(ParameterType.HEADER)
//                .query(q -> q.model(m -> m.scalarModel(ScalarType.STRING)))
//                .required(false)
//                .build());
        return parameters;
    }

    /**
     * 生成通用响应信息
     * @return
     */
    private List<Response> getGlobalResponseMessage() {
        List<Response> responseList = new ArrayList<>();
        responseList.add(new ResponseBuilder().code("4xx").description("请求错误，根据code和msg检查").build());
        return responseList;
    }

}
