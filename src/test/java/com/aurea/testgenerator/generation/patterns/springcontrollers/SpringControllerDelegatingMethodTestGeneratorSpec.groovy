package com.aurea.testgenerator.generation.patterns.springcontrollers

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator

class SpringControllerDelegatingMethodTestGeneratorSpec extends MatcherPipelineTest {

    def "simple service call should be verified"() {
        expect:
        onClassCodeExpect """
        import com.aurea.auth.generalpatternsspringtest.service.DelegateService;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;
        
        @RestController
        public class SimpleDelegatingController {
        
            public final DelegateService delegateService;
        
            public SimpleDelegatingController(DelegateService delegateService) {
                this.delegateService = delegateService;
            }
        
            @RequestMapping("/delegate")
            public void delegate() {
                delegateService.delegate();
            }
        
        }

        """, """     
        package sample;
 
        import com.aurea.auth.generalpatternsspringtest.service.DelegateService;
        import com.fasterxml.jackson.databind.ObjectMapper;
        import javax.annotation.Generated;
        import org.junit.Before;
        import org.junit.Test;
        import org.mockito.InjectMocks;
        import org.mockito.Mock;
        import org.mockito.Mockito;
        import org.mockito.MockitoAnnotations;
        import org.springframework.http.MediaType;
        import org.springframework.test.web.servlet.MockMvc;
        import org.springframework.test.web.servlet.setup.MockMvcBuilders;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;
        import static org.mockito.ArgumentMatchers.any;
        import static org.mockito.ArgumentMatchers.eq;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
        
        @Generated("GeneralPatterns")
        public class FooSpringControllerDelegatingMethodTest {
         
            @InjectMocks()
            private SimpleDelegatingController controllerInstance;
         
            @Mock()
            private DelegateService delegateService;
         
            private MockMvc mockMvc;
         
            @Before
            public void setup() {
                MockitoAnnotations.initMocks(this);
                mockMvc = MockMvcBuilders.standaloneSetup(controllerInstance).build();
            }
         
            @Test
            public void delegateDelegatesToService() throws Exception {
                mockMvc.perform(get("/delegate")).andExpect(status().is2xxSuccessful());
                Mockito.verify(delegateService).delegate();
            }
        }
        """
    }

    def "service call with parameters should be verified"() {
        expect:
        onClassCodeExpect """
        import com.aurea.auth.generalpatternsspringtest.service.DelegateService;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestParam;
        import org.springframework.web.bind.annotation.RestController;
        
        @RestController
        @RequestMapping(path = "/params")
        public class DelegatingWithParametersController {
        
            public final DelegateService delegateService;
        
            public DelegatingWithParametersController(DelegateService delegateService) {
                this.delegateService = delegateService;
            }
        
            @PostMapping("/delegate/{int_param}")
            public void delegate(@PathVariable(name = "int_param") int intParam, @RequestParam(name = "string_param") String
                    stringParam) {
                delegateService.delegateWithParameters(intParam, stringParam);
            }
        
        }

        """, """ 
        package sample;
 
        import com.aurea.auth.generalpatternsspringtest.service.DelegateService;
        import com.fasterxml.jackson.databind.ObjectMapper;
        import javax.annotation.Generated;
        import org.junit.Before;
        import org.junit.Test;
        import org.mockito.InjectMocks;
        import org.mockito.Mock;
        import org.mockito.Mockito;
        import org.mockito.MockitoAnnotations;
        import org.springframework.http.MediaType;
        import org.springframework.test.web.servlet.MockMvc;
        import org.springframework.test.web.servlet.setup.MockMvcBuilders;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestParam;
        import org.springframework.web.bind.annotation.RestController;
        import static org.mockito.ArgumentMatchers.any;
        import static org.mockito.ArgumentMatchers.eq;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
         
        @Generated("GeneralPatterns")
        public class FooSpringControllerDelegatingMethodTest {
         
            @InjectMocks()
            private DelegatingWithParametersController controllerInstance;
         
            @Mock()
            private DelegateService delegateService;
         
            private MockMvc mockMvc;
         
            @Before
            public void setup() {
                MockitoAnnotations.initMocks(this);
                mockMvc = MockMvcBuilders.standaloneSetup(controllerInstance).build();
            }
         
            @Test
            public void delegateDelegatesToService() throws Exception {
                int intParam = 42;
                String stringParam = "ABC";
                mockMvc.perform(post("/params/delegate/" + intParam + "").param("string_param", String.valueOf(stringParam))).andExpect(status().is2xxSuccessful());
                Mockito.verify(delegateService).delegateWithParameters(eq(intParam), eq(stringParam));
            }
        }
        """
    }

    def "service call returning values should be mocked"() {
        expect:
        onClassCodeExpect """
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestBody;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestMethod;
        import org.springframework.web.bind.annotation.RequestParam;
        import org.springframework.web.bind.annotation.RestController;
        import org.springframework.web.bind.annotation.RequestHeader;
        
        @RestController
        @RequestMapping(value = "/return")
        public class DelegatingWithReturnValueController {
        
            public final DelegateService delegateService;
        
            public DelegatingWithReturnValueController(DelegateService delegateService) {
                this.delegateService = delegateService;
            }
        
            @RequestMapping(value = "/delegate/{int_param}", method = RequestMethod.POST)
            public Value delegateWithValue(@PathVariable(name = "int_param") int intParam,
                    @RequestParam(name = "string_param") String stringParam, @RequestBody Body body,
                    @RequestHeader("User-Agent") String userAgent) {
                return delegateService.delegateWithValue(intParam, stringParam, body);
            }
        
        }
        
         class Value {
            private int intValue;
            private String stringValue;
        
            public Value(int intValue, String stringValue) {
                this.intValue = intValue;
                this.stringValue = stringValue;
            }
        
            public Value(){}
        
            public int getIntValue() {
                return intValue;
            }
        
            public void setIntValue(int intValue) {
                this.intValue = intValue;
            }
        
            public String getStringValue() {
                return stringValue;
            }
        
            public void setStringValue(String stringValue) {
                this.stringValue = stringValue;
            }
        }
        class Body {
            int id;
        
            public Body(int id) {
                this.id = id;
            }
        
            public Body() {
            }
        
            public int getId() {
                return id;
            }
        
            public void setId(int id) {
                this.id = id;
            }
        }
        
        
        class DelegateService {
            public Value delegateWithValue(int intParam, String stringParam, Body body) {
                return new Value(intParam,stringParam);
            }
        }

        """, """ 
        package sample;
 
        import com.fasterxml.jackson.databind.ObjectMapper;
        import javax.annotation.Generated;
        import org.junit.Before;
        import org.junit.Test;
        import org.mockito.InjectMocks;
        import org.mockito.Mock;
        import org.mockito.Mockito;
        import org.mockito.MockitoAnnotations;
        import org.springframework.http.MediaType;
        import org.springframework.test.web.servlet.MockMvc;
        import org.springframework.test.web.servlet.setup.MockMvcBuilders;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestBody;
        import org.springframework.web.bind.annotation.RequestHeader;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestMethod;
        import org.springframework.web.bind.annotation.RequestParam;
        import org.springframework.web.bind.annotation.RestController;
        import static org.mockito.ArgumentMatchers.any;
        import static org.mockito.ArgumentMatchers.eq;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
        
        @Generated("GeneralPatterns")
        public class FooSpringControllerDelegatingMethodTest {
         
            @InjectMocks()
            private DelegatingWithReturnValueController controllerInstance;
         
            @Mock()
            private DelegateService delegateService;
         
            private MockMvc mockMvc;
         
            @Before
            public void setup() {
                MockitoAnnotations.initMocks(this);
                mockMvc = MockMvcBuilders.standaloneSetup(controllerInstance).build();
            }
         
            @Test
            public void delegateWithValueDelegatesToService() throws Exception {
                int intParam = 42;
                String stringParam = "ABC";
                Body body = new Body();
                String userAgent = "ABC";
                ObjectMapper mapper = new ObjectMapper();
                Value expectedResult = new Value();
                Mockito.when(delegateService.delegateWithValue(eq(intParam), eq(stringParam), any(Body.class))).thenReturn(expectedResult);
                mockMvc.perform(post("/return/delegate/" + intParam + "").content(mapper.writeValueAsString(body)).contentType("application/json;charset=UTF-8").param("string_param", String.valueOf(stringParam)).header("User-Agent", String.valueOf(userAgent))).andExpect(status().is2xxSuccessful());
                Mockito.verify(delegateService).delegateWithValue(eq(intParam), eq(stringParam), any(Body.class));
            }
        }
        """
    }

    @Override
    TestGenerator generator() {
        return new SpringControllerDelegatingMethodTestGenerator(solver, reporter, visitReporter, nomenclatureFactory,
                valueFactory, cfg)
    }
}
