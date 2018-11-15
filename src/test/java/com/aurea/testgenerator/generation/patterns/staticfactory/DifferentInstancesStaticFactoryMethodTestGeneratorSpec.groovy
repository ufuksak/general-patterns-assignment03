package com.aurea.testgenerator.generation.patterns.staticfactory

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator

class DifferentInstancesStaticFactoryMethodTestGeneratorSpec extends MatcherPipelineTest {

    def "static factory method should create different instances on consecutive invocation"() {
        expect:
        onClassCodeExpect """
            class Foo {
                public static Foo newFoo(String username) {
                    return new Foo();
                }   
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
            
            @Generated("GeneralPatterns")
            public class FooDifferentInstancesStaticFactoryMethodTest {
             
                @Test
                public void newFooOnSecondCallCreateDifferentInstance() throws Exception {
                    Foo first = Foo.newFoo("ABC");
                    Foo other = Foo.newFoo("ABC");
                    
                    assertThat(first).isNotSameAs(other);
                }
            }
        """
    }

    def "static factory method should create different instances on consecutive invocation for args constructors"() {
        expect:
        onClassCodeExpect """
            class Foo {
                private final String username;
                
                Foo(String username) {
                    this.username = username;
                }
                
                public static Foo newFoo(String username) {
                    return new Foo(username);
                }   
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooDifferentInstancesStaticFactoryMethodTest {
             
                @Test
                public void newFooOnSecondCallCreateDifferentInstance() throws Exception {
                    Foo first = Foo.newFoo("ABC");
                    Foo other = Foo.newFoo("ABC");
                    
                    assertThat(first).isNotSameAs(other);
                }
            }
        """
    }

    @Override
    TestGenerator generator() {
        new DifferentInstancesStaticFactoryMethodTestGenerator(solver, reporter, visitReporter, nomenclatureFactory, valueFactory, cfg)
    }
}
