package com.aurea.testgenerator.generation.patterns.singleton

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator

class StaticBlockSingletonTestGeneratorSpec extends MatcherPipelineTest {

    def "static block same instance test"() {
        expect:
        onClassCodeExpect """
            class Foo {
                private static Foo instance;
            
                static {
                    try {
                        instance = new Foo();
                    } catch (Exception e) {
                        throw new RuntimeException("Exception occurred in creating singleton instance");
                    }
                }
            
                private Foo() {
                }
            
                public static Foo getInstance() {
                    return instance;
                }
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooStaticBlockSingletonTest {
             
                @Test
                public void getInstanceOnSecondCallReturnsSameInstance() throws Exception {
                    Foo first = Foo.getInstance();
                    Foo other = Foo.getInstance();
                    
                    assertThat(first).isSameAs(other);
                }
            }
        """
    }

    @Override
    TestGenerator generator() {
        new StaticBlockSingletonTestGenerator(solver, reporter, visitReporter, nomenclatureFactory,
                new SingletonCommonTestGenerator(valueFactory, nomenclatureFactory), cfg)
    }
}
