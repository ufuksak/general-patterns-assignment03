package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator
import com.aurea.testgenerator.value.MockValueFactory


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
 
        import javax.annotation.Generated;
        import org.junit.Test;
        import org.springframework.test.util.ReflectionTestUtils;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Mockito.*;
         
        @Generated("GeneralPatterns")
        public class FooPatternTest {
         
            @Test
            public void testMethod1() {
                // arrange
                Foo classInstance = spy(Foo.class);
                doNothing().when(classInstance).method1Impl();
                // act
                classInstance.method1();
                // assert
                verify(classInstance, atLeast(1)).method1Impl();
            }
         
            @Test
            public void testMethod1_2() {
                // arrange
                Foo classInstance = spy(Foo.class);
                doNothing().when(classInstance).method1Impl(null);
                // act
                classInstance.method1(42);
                // assert
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
         
            @Test
            public void testMethod2() {
                // arrange
                Foo classInstance = spy(Foo.class);
                Foo delegate_foo = mock(Foo.class);
                doNothing().when(delegate_foo).method1();
                ReflectionTestUtils.setField(classInstance, "foo", delegate_foo);
                // act
                classInstance.method2();
                // assert
                verify(delegate_foo, atLeast(1)).method1();
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
         
        import javax.annotation.Generated;
        import org.junit.Test;
        import org.springframework.test.util.ReflectionTestUtils;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Mockito.*;
         
        @Generated("GeneralPatterns")
        public class FooPatternTest {
         
            @Test
            public void testMethod1() {
                // arrange
                Foo classInstance = spy(Foo.class);
                doNothing().when(classInstance).method1Impl();
                Object expected_1 = mock(Object.class, RETURNS_DEEP_STUBS);
                doReturn(expected_1).when(classInstance).method1Impl(null);
                // act
                classInstance.method1();
                // assert
                verify(classInstance, atLeast(1)).method1Impl();
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
         
            @Test
            public void testMethod1_2() {
                // arrange
                Foo classInstance = spy(Foo.class);
                Object expected_1 = mock(Object.class, RETURNS_DEEP_STUBS);
                doReturn(expected_1).when(classInstance).method1Impl(null);
                Object expected_2 = mock(Object.class, RETURNS_DEEP_STUBS);
                doReturn(expected_2).when(classInstance).method1Impl(null);
                // act
                Object actual = classInstance.method1(42, "ABC", 42.0, true, new Foo());
                assertEquals(expected_2, actual);
                // assert
                verify(classInstance, atLeast(1)).method1Impl(null);
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
        }
        """
    }

    def "Delegate is a parameter or variable methods tests generation"() {
        expect:
        onClassCodeExpect """
        class Foo {
            Foo localFoo = new Foo();

            public void method1(int param1, Foo delegate) {
                delegate.method1Impl(param1);
                new Foo().method1Impl2nd(param1);
            }
        
            public int method2(int param1) {
                return localFoo.method1Impl(param1);
            }
        
            int method1Impl(int value) {
                return value;
            }
        
            Object method1Impl2nd(Object obj) {
                return new Object();
            }
        }
        """, """package sample;
 
        import javax.annotation.Generated;
        import org.junit.Test;
        import org.springframework.test.util.ReflectionTestUtils;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Mockito.*;
         
        @Generated("GeneralPatterns")
        public class FooPatternTest {
         
            @Test
            public void testMethod1() {
                // arrange
                Foo classInstance = spy(Foo.class);
                Foo delegate_delegate = mock(Foo.class);
                int expected_1 = 42;
                doReturn(expected_1).when(delegate_delegate).method1Impl(anyInt());
                // act
                classInstance.method1(42, delegate_delegate);
                // assert
                verify(delegate_delegate, atLeast(1)).method1Impl(42);
            }
        
            @Test
            public void testMethod2() {
                // arrange
                Foo classInstance = spy(Foo.class);
                Foo delegate_localFoo = mock(Foo.class);
                int expected_1 = 42;
                doReturn(expected_1).when(delegate_localFoo).method1Impl(anyInt());
                ReflectionTestUtils.setField(classInstance, "localFoo", delegate_localFoo);
                // act
                int actual = classInstance.method2(42);
                assertEquals(expected_1, actual);
                // assert
                verify(delegate_localFoo, atLeast(1)).method1Impl(42);
            }
        }
        """
    }

    @Override
    TestGenerator generator() {
        new DelegateTestGenerator(solver, reporter, visitReporter, nomenclatureFactory, valueFactory, new MockValueFactory())
    }
}
