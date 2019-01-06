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
 
        import java.lang.reflect.Field;
        import javax.annotation.Generated;
        import org.junit.Test;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Matchers.any;
        import static org.mockito.Matchers.anyBoolean;
        import static org.mockito.Matchers.anyDouble;
        import static org.mockito.Matchers.anyFloat;
        import static org.mockito.Matchers.anyInt;
        import static org.mockito.Matchers.anyLong;
        import static org.mockito.Matchers.anyString;
        import static org.mockito.Mockito.atLeast;
        import static org.mockito.Mockito.doNothing;
        import static org.mockito.Mockito.doReturn;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.spy;
        import static org.mockito.Mockito.verify;
         
        @Generated("GeneralPatterns")
        public class FooDelegateTest {
         
            @Test
            public void testMethod1() throws Exception {
                // arrange
                Foo class_property_foo = mock(Foo.class);
                Foo classInstance = mock(Foo.class);
                Field field_foo = Foo.class.getDeclaredField("foo");
                field_foo.setAccessible(true);
                field_foo.set(classInstance, class_property_foo);
                doNothing().when(classInstance).method1Impl();
                // act
                classInstance.method1();
                // assert
                verify(classInstance, atLeast(1)).method1Impl();
            }
         
            @Test
            public void testMethod1_2() throws Exception {
                // arrange
                int param1 = 40;
                Foo class_property_foo = mock(Foo.class);
                Foo classInstance = mock(Foo.class);
                Field field_foo = Foo.class.getDeclaredField("foo");
                field_foo.setAccessible(true);
                field_foo.set(classInstance, class_property_foo);
                doNothing().when(classInstance).method1Impl(null);
                // act
                classInstance.method1(param1);
                // assert
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
         
            @Test
            public void testMethod2() throws Exception {
                // arrange
                Foo class_property_foo = mock(Foo.class);
                Foo classInstance = new Foo();
                Field field_foo = Foo.class.getDeclaredField("foo");
                field_foo.setAccessible(true);
                field_foo.set(classInstance, class_property_foo);
                doNothing().when(class_property_foo).method1();
                // act
                classInstance.method2();
                // assert
                verify(class_property_foo, atLeast(1)).method1();
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
         
        import java.lang.reflect.Field;
        import javax.annotation.Generated;
        import org.junit.Test;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Matchers.any;
        import static org.mockito.Matchers.anyBoolean;
        import static org.mockito.Matchers.anyDouble;
        import static org.mockito.Matchers.anyFloat;
        import static org.mockito.Matchers.anyInt;
        import static org.mockito.Matchers.anyLong;
        import static org.mockito.Matchers.anyString;
        import static org.mockito.Mockito.atLeast;
        import static org.mockito.Mockito.doNothing;
        import static org.mockito.Mockito.doReturn;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.spy;
        import static org.mockito.Mockito.verify;
         
        @Generated("GeneralPatterns")
        public class FooDelegateTest {
         
            @Test
            public void testMethod1() throws Exception {
                // arrange
                Foo classInstance = mock(Foo.class);
                doNothing().when(classInstance).method1Impl();
                doNothing().when(classInstance).method1Impl(null);
                // act
                classInstance.method1();
                // assert
                verify(classInstance, atLeast(1)).method1Impl();
                verify(classInstance, atLeast(1)).method1Impl(null);
            }
         
            @Test
            public void testMethod1_2() throws Exception {
                // arrange
                int param1 = 40;
                String param2 = "Vice Versa";
                Double param3 = mock(Double.class);
                boolean param4 = false;
                Foo param5 = mock(Foo.class);
                Object expected = mock(Object.class);
                Foo classInstance = mock(Foo.class);
                doNothing().when(classInstance).method1Impl(null);
                doReturn(expected).when(classInstance).method1Impl(null);
                // act
                Object actual = classInstance.method1(param1, param2, param3, param4, param5);
                // assert
                assertEquals(expected, actual);
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
 
        import java.lang.reflect.Field;
        import javax.annotation.Generated;
        import org.junit.Test;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Matchers.any;
        import static org.mockito.Matchers.anyBoolean;
        import static org.mockito.Matchers.anyDouble;
        import static org.mockito.Matchers.anyFloat;
        import static org.mockito.Matchers.anyInt;
        import static org.mockito.Matchers.anyLong;
        import static org.mockito.Matchers.anyString;
        import static org.mockito.Mockito.atLeast;
        import static org.mockito.Mockito.doNothing;
        import static org.mockito.Mockito.doReturn;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.spy;
        import static org.mockito.Mockito.verify;
         
        @Generated("GeneralPatterns")
        public class FooDelegateTest {
         
            @Test
            public void testMethod1() throws Exception {
                // arrange
                int param1 = 40;
                Foo delegate = mock(Foo.class);
                Foo class_property_localFoo = mock(Foo.class);
                Foo classInstance = new Foo();
                Field field_localFoo = Foo.class.getDeclaredField("localFoo");
                field_localFoo.setAccessible(true);
                field_localFoo.set(classInstance, class_property_localFoo);
                doNothing().when(delegate).method1Impl(param1);
                // act
                classInstance.method1(param1, delegate);
                // assert
                verify(delegate, atLeast(1)).method1Impl(param1);
            }
         
            @Test
            public void testMethod2() throws Exception {
                // arrange
                int param1 = 40;
                int expected = 40;
                Foo class_property_localFoo = mock(Foo.class);
                Foo classInstance = new Foo();
                Field field_localFoo = Foo.class.getDeclaredField("localFoo");
                field_localFoo.setAccessible(true);
                field_localFoo.set(classInstance, class_property_localFoo);
                doReturn(expected).when(class_property_localFoo).method1Impl(param1);
                // act
                int actual = classInstance.method2(param1);
                // assert
                assertEquals(expected, actual);
                verify(class_property_localFoo, atLeast(1)).method1Impl(param1);
            }
        }
        """
    }

    def "Delegate is a return value of prev call tests generation"() {
        expect:
        onClassCodeExpect """
        class Foo {
            private long localParam = 5;
            private FooDelegate fooDelegate = new FooDelegate();
        
            public long method1(long param1, FooDelegate delegate) {
                long value = this.method1Impl1st(param1);
                delegate.method1Impl2nd(value);
                return value;
            }
        
            public long method2() {
                long value = method1Impl1st(this.localParam);
                this.method1Impl1st(value);
                return fooDelegate.method1Impl2nd(value); 
            }

            public long method1Impl1st(long value) {
                return value * 2;
            }
        }
        
        class FooDelegate {
            public long method1Impl2nd(long value) {
                return value + 100;
            }
        }
        """, """package sample;
 
        import java.lang.reflect.Field;
        import javax.annotation.Generated;
        import org.junit.Test;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Matchers.any;
        import static org.mockito.Matchers.anyBoolean;
        import static org.mockito.Matchers.anyDouble;
        import static org.mockito.Matchers.anyFloat;
        import static org.mockito.Matchers.anyInt;
        import static org.mockito.Matchers.anyLong;
        import static org.mockito.Matchers.anyString;
        import static org.mockito.Mockito.atLeast;
        import static org.mockito.Mockito.doNothing;
        import static org.mockito.Mockito.doReturn;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.spy;
        import static org.mockito.Mockito.verify;
         
        @Generated("GeneralPatterns")
        public class FooDelegateTest {
         
            @Test
            public void testMethod1() throws Exception {
                // arrange
                long param1 = 50;
                FooDelegate delegate = mock(FooDelegate.class);
                long expected = 50;
                long class_property_localParam = 50;
                FooDelegate class_property_fooDelegate = mock(FooDelegate.class);
                Foo classInstance = mock(Foo.class);
                Field field_localParam = Foo.class.getDeclaredField("localParam");
                field_localParam.setAccessible(true);
                field_localParam.set(classInstance, class_property_localParam);
                Field field_fooDelegate = Foo.class.getDeclaredField("fooDelegate");
                field_fooDelegate.setAccessible(true);
                field_fooDelegate.set(classInstance, class_property_fooDelegate);
                long expectedvalue = 50;
                doReturn(expectedvalue).when(classInstance).method1Impl1st(param1);
                doNothing().when(delegate).method1Impl2nd(expectedvalue);
                // act
                long actual = classInstance.method1(param1, delegate);
                // assert
                assertEquals(expectedvalue, actual);
                verify(classInstance, atLeast(1)).method1Impl1st(param1);
                verify(delegate, atLeast(1)).method1Impl2nd(expectedvalue);
            }
         
            @Test
            public void testMethod2() throws Exception {
                // arrange
                long expected = 50;
                long class_property_localParam = 50;
                FooDelegate class_property_fooDelegate = mock(FooDelegate.class);
                Foo classInstance = mock(Foo.class);
                Field field_localParam = Foo.class.getDeclaredField("localParam");
                field_localParam.setAccessible(true);
                field_localParam.set(classInstance, class_property_localParam);
                Field field_fooDelegate = Foo.class.getDeclaredField("fooDelegate");
                field_fooDelegate.setAccessible(true);
                field_fooDelegate.set(classInstance, class_property_fooDelegate);
                long expectedvalue = 50;
                doReturn(expectedvalue).when(classInstance).method1Impl1st(class_property_localParam);
                doNothing().when(classInstance).method1Impl1st(expectedvalue);
                doReturn(expected).when(class_property_fooDelegate).method1Impl2nd(expectedvalue);
                // act
                long actual = classInstance.method2();
                // assert
                assertEquals(expected, actual);
                verify(classInstance, atLeast(1)).method1Impl1st(class_property_localParam);
                verify(classInstance, atLeast(1)).method1Impl1st(expectedvalue);
                verify(class_property_fooDelegate, atLeast(1)).method1Impl2nd(expectedvalue);
            }
        }
        """
    }

    def "Delegate to call with parameter expression tests generation"() {
        expect:
        onClassCodeExpect """
        class Foo {
            private long localParam = 5;
            private final static String CONST = "Const";
        
            public void method1(String param1) {
                String localString = "localString";
                method1Impl(param1 + CONST + this.localParam + localString);
            }
        
            private String method1Impl(String param) {
                return param;
            }
        }
        """, """package sample;
 
        import java.lang.reflect.Field;
        import javax.annotation.Generated;
        import org.junit.Test;
        import static org.junit.Assert.assertEquals;
        import static org.mockito.Matchers.any;
        import static org.mockito.Matchers.anyBoolean;
        import static org.mockito.Matchers.anyDouble;
        import static org.mockito.Matchers.anyFloat;
        import static org.mockito.Matchers.anyInt;
        import static org.mockito.Matchers.anyLong;
        import static org.mockito.Matchers.anyString;
        import static org.mockito.Mockito.atLeast;
        import static org.mockito.Mockito.doNothing;
        import static org.mockito.Mockito.doReturn;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.spy;
        import static org.mockito.Mockito.verify;
         
        @Generated("GeneralPatterns")
        public class FooDelegateTest {
         
            @Test
            public void testMethod1() throws Exception {
                // arrange
                String param1 = "Vice Versa";
                long class_property_localParam = 50;
                String class_property_CONST = "Vice Versa";
                Foo classInstance = mock(Foo.class);
                Field field_localParam = Foo.class.getDeclaredField("localParam");
                field_localParam.setAccessible(true);
                field_localParam.set(classInstance, class_property_localParam);
                Field field_CONST = Foo.class.getDeclaredField("CONST");
                field_CONST.setAccessible(true);
                field_CONST.set(classInstance, class_property_CONST);
                String expectedlocalString = "Vice Versa";
                doNothing().when(classInstance).method1Impl(expectedparam1 + CONST + this.localParam + localString);
                // act
                classInstance.method1(param1);
                // assert
                verify(classInstance, atLeast(1)).method1Impl(expectedparam1 + CONST + this.localParam + localString);
            }
        }
        """
    }

    @Override
    TestGenerator generator() {
        new DelegateTestGenerator(solver, reporter, visitReporter, nomenclatureFactory, valueFactory, new MockValueFactory(), cfg)
    }
}
