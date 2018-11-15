package com.aurea.testgenerator.generation.patterns.singleton

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator

class EagerSingletonTestGeneratorSpec extends MatcherPipelineTest {

    def "eager singleton same instance test"() {
        expect:
        onClassCodeExpect """
            class Foo {
                private static final Foo instance = new Foo();
            
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
            public class FooEagerSingletonTest {
             
                @Test
                public void getInstanceOnSecondCallReturnsSameInstance() throws Exception {
                    Foo first = Foo.getInstance();
                    Foo other = Foo.getInstance();
                    
                    assertThat(first).isSameAs(other);
                }
            }
        """
    }

    def "eager singleton type reference same instance test"() {
        expect:
        onClassCodeExpect """
            class Foo {
                private static final Foo instance = new Foo();
            
                private Foo() {
                }
            
                public static Foo getInstance() {
                    return Foo.instance;
                }            
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooEagerSingletonTest {
             
                @Test
                public void getInstanceOnSecondCallReturnsSameInstance() throws Exception {
                    Foo first = Foo.getInstance();
                    Foo other = Foo.getInstance();
                    
                    assertThat(first).isSameAs(other);
                }
            }
        """
    }

    def "Bill Pugh eager singleton same instance test"() {
        expect:
        onClassCodeExpect """
            class Foo {
                private Foo() {
                }

                public static Foo getInstance() {
                    return SingletonHelper.INSTANCE;
                }
            
                private static class SingletonHelper {
                    private static final Foo INSTANCE = new Foo();
                }            
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooEagerSingletonTest {
             
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
        new EagerSingletonTestGenerator(solver, reporter, visitReporter, nomenclatureFactory,
                new SingletonCommonTestGenerator(valueFactory, nomenclatureFactory), cfg)
    }
}
