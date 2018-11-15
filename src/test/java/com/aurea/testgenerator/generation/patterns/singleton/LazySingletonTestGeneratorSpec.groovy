package com.aurea.testgenerator.generation.patterns.singleton

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator

class LazySingletonTestGeneratorSpec extends MatcherPipelineTest {

    def "lazy singleton tests"() {
        expect:
        onClassCodeExpect """
            class Foo {
                private static Foo instance;

                private Foo() {
                }
            
                public static Foo getInstance() {
                    if (instance == null) {
                        instance = new Foo();
                    }
                    return instance;
                }         
            }
            """, """     
            package sample;

            import com.aurea.unittest.commons.SingletonTester;
            import java.util.concurrent.Callable;
            import javax.annotation.Generated;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooLazySingletonTest {
                
                @Test
                public void getInstanceOnSecondCallReturnsSameInstance() throws Exception {
                    Foo first = Foo.getInstance();
                    Foo other = Foo.getInstance();
                    assertThat(first).isSameAs(other);
                }
            
                @Test
                public void getInstanceIsThreadSafe() throws Exception {
                    SingletonTester tester = SingletonTester.fromSingleton(new Callable<Foo>() {
            
                        @Override
                        public Foo call() throws Exception {
                            return Foo.getInstance();
                        }
                    });
                    tester.testThreadSafety();
                }
            }
        """
    }

    @Override
    TestGenerator generator() {
        new LazySingletonTestGenerator(solver, reporter, visitReporter, nomenclatureFactory,
                new SingletonCommonTestGenerator(valueFactory, nomenclatureFactory), cfg)
    }
}
