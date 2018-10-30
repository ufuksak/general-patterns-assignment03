package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator


class DelegateTestGeneratorSpec extends MatcherPipelineTest {

    def "Simple delegate methods tests generation"() {
        expect:
        onClassCodeExpect """
        class Foo {
            Foo foo = new Foo();        
        
            public void method1() {
                method1Impl();
            }
            
            public void method1(int param1) {
                this.method1Impl(null);
            }
            
            public void method2() {
                foo.method1();
            }

            private void method1Impl() {}
            private void method1Impl(Object obj) {}
        }
        """, """package sample;

        import com.openpojo.reflection.impl.PojoClassFactory;
        import com.openpojo.validation.utils.ValidationHelper;
        import javax.annotation.Generated;
        import org.junit.Test;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Mockito.any;
        import static org.mockito.Mockito.atLeast;
        import static org.mockito.Mockito.doNothing;
        import static org.mockito.Mockito.doReturn;
        import static org.mockito.Mockito.spy;
        import static org.mockito.Mockito.verify;

        @Generated("GeneralPatterns")
        public class FooPatternTest {

            @Test
            public void testMethod1() {
                // arrange
                Foo classInstance = spy((Foo) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(Foo.class)));
                doNothing().when(classInstance).method1Impl();
                // act
                classInstance.method1();
                // assert
                verify(classInstance, atLeast(1)).method1Impl();
            }
            
            @Test
            public void testMethod1_2() {
                // arrange
                Foo classInstance = spy((Foo) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(Foo.class)));
                doNothing().when(classInstance).method1Impl(any());
                // act
                classInstance.method1(42);
                // assert
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
        }
        """
    }

    def "Return value delegate methods tests generation"() {
        expect:
        onClassCodeExpect """
        class Foo {
            public void method1() {
                method1Impl();
                method1Impl(null);
            }
            
            public Object method1(int param1, String param2, Double param3, boolean param4, Foo param5) {
                method1Impl(null);
                return this.method1Impl(null);
            }
            
            private void method1Impl() {}
            private Object method1Impl(Object obj) {
                return new Object();
            }
        }
        """, """package sample;

        import com.openpojo.reflection.impl.PojoClassFactory;
        import com.openpojo.validation.utils.ValidationHelper;
        import javax.annotation.Generated;
        import org.junit.Test;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Mockito.any;
        import static org.mockito.Mockito.atLeast;
        import static org.mockito.Mockito.doNothing;
        import static org.mockito.Mockito.doReturn;
        import static org.mockito.Mockito.spy;
        import static org.mockito.Mockito.verify;

        @Generated("GeneralPatterns")
        public class FooPatternTest {

            @Test
            public void testMethod1() {
                // arrange
                Foo classInstance = spy((Foo) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(Foo.class)));
                doNothing().when(classInstance).method1Impl();
                Object expected1 = (Object) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(Object.class));
                doReturn(expected1).when(classInstance).method1Impl(any());
                // act
                classInstance.method1();
                // assert
                verify(classInstance, atLeast(1)).method1Impl();
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
         
            @Test
            public void testMethod1_2() {
                // arrange
                Foo classInstance = spy((Foo) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(Foo.class)));
                Object expected1 = (Object) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(Object.class));
                doReturn(expected1).when(classInstance).method1Impl(any());
                Object expected2 = (Object) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(Object.class));
                doReturn(expected2).when(classInstance).method1Impl(any());
                // act
                Object actual = classInstance.method1(42, "ABC", 42.0, true, new Foo());
                assertEquals(actual, expected2);
                // assert
                verify(classInstance, atLeast(1)).method1Impl(null);
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
        }
        """
    }

    @Override
    TestGenerator generator() {
        new DelegateTestGenerator(solver, reporter, visitReporter, nomenclatureFactory, valueFactory)
    }
}
