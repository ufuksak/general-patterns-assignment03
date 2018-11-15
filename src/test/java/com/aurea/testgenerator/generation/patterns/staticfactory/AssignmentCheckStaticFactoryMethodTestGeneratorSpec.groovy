package com.aurea.testgenerator.generation.patterns.staticfactory

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.MethodLevelTestGenerator

class AssignmentCheckStaticFactoryMethodTestGeneratorSpec extends MatcherPipelineTest {

    def "assigning default values should be asserted if fields are available"() {
        expect:
        onClassCodeExpect """
            class UserProfile {
    
                String username;
                int age;
                float degrees;
                boolean female;
                Object status;
                
                public static UserProfile emptyProfile(String username) {
                    UserProfile profile = new UserProfile();
                    profile.username = username;
                    profile.age = 42;
                    profile.degrees = 42.2F;
                    profile.female = false;
                    profile.status = new Object();
                    return profile;
                }   
            }
        """, """     
            package sample;

             
            import javax.annotation.Generated;
            import org.assertj.core.api.SoftAssertions;
            import org.assertj.core.data.Offset;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooAssignmentCheckStaticFactoryMethodTest {
             
                @Test
                public void emptyProfileAssignsValues() throws Exception {
                    String username = "ABC";
                    UserProfile resultingInstance = UserProfile.emptyProfile(username);
                    SoftAssertions sa = new SoftAssertions();
                    sa.assertThat(resultingInstance.female).isFalse();
                    sa.assertThat(resultingInstance.degrees).isCloseTo(42.2F, Offset.offset(0.001F));
                    sa.assertThat(resultingInstance.age).isEqualTo(42);
                    sa.assertThat(resultingInstance.status).isEqualTo(new Object());
                    sa.assertThat(resultingInstance.username).isEqualTo(username);
                    sa.assertAll();
                }
            }
        """
    }

    def "assigning values via setters should be checked"() {
        expect:
        onClassCodeExpect """
            class UserProfile {
    
                private String username;
                private int age;
                private float degrees;
                private boolean female;
                private Object status;
                
                public static UserProfile emptyProfile(String username) {
                    UserProfile profile = new UserProfile();
                    profile.setUsername(username);
                    profile.setAge(42);
                    profile.setDegrees(42.2F);
                    profile.setFemale(false);
                    profile.setStatus(new Object());
                    return profile;
                }
                
                public void setUsername(String username) {
                    this.username = username;
                }
                
                public void setAge(int age) {
                    this.age = age;
                }
                
                public void setDegrees(float degrees) {
                    this.degrees = degrees;
                }
                
                public void setFemale(boolean female) {
                    this.female = female;
                }
                
                public void setStatus(Object status) {
                    this.status = status;
                }
                
                public String getUsername() {
                    return username;
                }
                
                public int getAge() {
                    return age;
                }
                
                public float getDegrees() {
                    return degrees;
                }
                
                public boolean isFemale() {
                    return female;
                }
                
                public Object getStatus() {
                    return status;
                }
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.assertj.core.api.SoftAssertions;
            import org.assertj.core.data.Offset;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
            
            @Generated("GeneralPatterns")
            public class FooAssignmentCheckStaticFactoryMethodTest {

             
                @Test
                public void emptyProfileAssignsValues() throws Exception {
                    String username = "ABC";
                    UserProfile resultingInstance = UserProfile.emptyProfile(username);
                    SoftAssertions sa = new SoftAssertions();
                    sa.assertThat(resultingInstance.isFemale()).isFalse();
                    sa.assertThat(resultingInstance.getDegrees()).isCloseTo(42.2F, Offset.offset(0.001F));
                    sa.assertThat(resultingInstance.getAge()).isEqualTo(42);
                    sa.assertThat(resultingInstance.getStatus()).isEqualTo(new Object());
                    sa.assertThat(resultingInstance.getUsername()).isEqualTo(username);
                    sa.assertAll();
                }
            }
        """
    }

    def "mixing setters and assignments is tested"() {
        expect:
        onClassCodeExpect """
            class UserProfile {
    
                String username;
                private int age;
                
                public static UserProfile emptyProfile(String username) {
                    UserProfile profile = new UserProfile();
                    profile.username = username;
                    profile.setAge(42);
                    return profile;
                }
                
                public void setAge(int age) {
                    this.age = age;
                }
                
                public int getAge() {
                    return age;
                }
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.assertj.core.api.SoftAssertions;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooAssignmentCheckStaticFactoryMethodTest {
             
                @Test
                public void emptyProfileAssignsValues() throws Exception {
                    String username = "ABC";
                    UserProfile resultingInstance = UserProfile.emptyProfile(username);
                    SoftAssertions sa = new SoftAssertions();
                    sa.assertThat(resultingInstance.getAge()).isEqualTo(42);
                    sa.assertThat(resultingInstance.username).isEqualTo(username);
                    sa.assertAll();
                }
            }
        """
    }

    def "assignments in constructor should be tested"() {
        expect:
        onClassCodeExpect """
            class Foo {
    
                String username;
                private int age;
                
                public Foo(String username, int age) {
                    this.username = username;
                    setAge(age);
                }
                
                public static Foo emptyProfile() {
                    Foo foo = new Foo("Galatae, quadra!", 58);
                    return foo;
                }
                
                public void setAge(int age) {
                    this.age = age;
                }
                
                public int getAge() {
                    return age;
                }
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.assertj.core.api.SoftAssertions;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooAssignmentCheckStaticFactoryMethodTest {
             
                @Test
                public void emptyProfileAssignsValues() throws Exception {
                    Foo resultingInstance = Foo.emptyProfile();
                    SoftAssertions sa = new SoftAssertions();
                    sa.assertThat(resultingInstance.getAge()).isEqualTo(58);
                    sa.assertThat(resultingInstance.username).isEqualTo("Galatae, quadra!");
                    sa.assertAll();
                }
            }
        """
    }

    def "assignments in constructor should be tested when object is created in return expr"() {
        expect:
        onClassCodeExpect """
            class Foo {
    
                String username;
                private int age;
                
                public Foo(String username, int age) {
                    this.username = username;
                    setAge(age);
                }
                
                public static Foo emptyProfile() {
                    return new Foo("Galatae, quadra!", 58);
                }
                
                public void setAge(int age) {
                    this.age = age;
                }
                
                public int getAge() {
                    return age;
                }
            }
        """, """     
            package sample;
            import javax.annotation.Generated;
            import org.assertj.core.api.SoftAssertions;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooAssignmentCheckStaticFactoryMethodTest {
             
                @Test
                public void emptyProfileAssignsValues() throws Exception {
                    Foo resultingInstance = Foo.emptyProfile();
                    SoftAssertions sa = new SoftAssertions();
                    sa.assertThat(resultingInstance.getAge()).isEqualTo(58);
                    sa.assertThat(resultingInstance.username).isEqualTo("Galatae, quadra!");
                    sa.assertAll();
                }
            }
        """
    }

    def "assignments in constructor should be tested when static factory method delegates arguments to constructor"() {
        expect:
        onClassCodeExpect """
            class Foo {
    
                String username;
                private int age;
                
                public Foo(String username, int age) {
                    this.username = username;
                    setAge(age);
                }
                
                public static Foo emptyProfile(String username) {
                    return new Foo(username, 58);
                }
                
                public void setAge(int age) {
                    this.age = age;
                }
                
                public int getAge() {
                    return age;
                }
            }
        """, """     
            package sample;
             
            import javax.annotation.Generated;
            import org.assertj.core.api.SoftAssertions;
            import org.junit.Test;
            import static org.assertj.core.api.Assertions.assertThat;
             
            @Generated("GeneralPatterns")
            public class FooAssignmentCheckStaticFactoryMethodTest {
             
                @Test
                public void emptyProfileAssignsValues() throws Exception {
                    String username = "ABC";
                    Foo resultingInstance = Foo.emptyProfile(username);
                    SoftAssertions sa = new SoftAssertions();
                    sa.assertThat(resultingInstance.getAge()).isEqualTo(58);
                    sa.assertThat(resultingInstance.username).isEqualTo(username);
                    sa.assertAll();
                }
            }
        """
    }

    @Override
    MethodLevelTestGenerator generator() {
        new AssignmentCheckStaticFactoryMethodTestGenerator(solver,
                reporter,
                visitReporter,
                nomenclatureFactory,
                valueFactory,
                softAssertions,
                cfg)
    }
}
