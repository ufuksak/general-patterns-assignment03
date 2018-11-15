package com.aurea.testgenerator.generation.patterns.springrepository

import com.aurea.testgenerator.MatcherPipelineTest
import com.aurea.testgenerator.generation.TestGenerator

class SpringRepositoryTestGeneratorSpec extends MatcherPipelineTest {

    def "Bill Pugh spring repository test"() {
        expect:
        onClassCodeExpect """
            @Repository
            public interface BillPughRepository extends JpaRepository<BillPugh, Long> {
            
                @Autowired
                private TestEntityManager entityManager;
            
                BillPugh findByName(String name);
            
                BillPugh findFirstBySourceUrlAndCommitOrderByDateUpdatedDesc(String sourceUrl, String commit);
            
                List<BillPugh> findAllByNeedRun(Boolean needRun);
            
            }
        """, """
            package sample;

            import javax.annotation.Generated;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
            import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
            import org.springframework.test.context.junit4.SpringRunner;
            import static org.assertj.core.api.Assertions.assertThat;
            
            @Generated("GeneralPatterns")
            @RunWith(SpringRunner.class)
            @DataJpaTest
            public class FooSpringRepositoryTest {
            
                @Autowired()
                private TestEntityManager entityManager;
            
                @Autowired()
                private BillPughRepository repository;
            
                @Test
                public void findByNameReturnsEntity() throws Exception {
                    // given
                    BillPugh entity = new BillPugh();
                    String name = "ABC";
                    entity.setName(name);
                    entityManager.persist(entity);
                    entityManager.flush();
                    // when
                    BillPugh found = repository.findByName(name);
                    // then
                    assertThat(found.getName()).isEqualTo(entity.getName());
                }
             
                @Test
                public void findFirstBySourceUrlAndCommitOrderByDateUpdatedDescReturnsEntity() throws Exception {
                    // given
                    BillPugh entity = new BillPugh();
                    String sourceUrl = "ABC";
                    String commit = "ABC";
                    entity.setSourceUrl(sourceUrl);
                    entity.setCommit(commit);
                    entityManager.persist(entity);
                    entityManager.flush();
                    // when
                    BillPugh found = repository.findFirstBySourceUrlAndCommitOrderByDateUpdatedDesc(sourceUrl, commit);
                    // then
                    assertThat(found.getSourceUrl()).isEqualTo(entity.getSourceUrl());
                }
             
                @Test
                public void findAllByNeedRunReturnsEntity() throws Exception {
                    // given
                    BillPugh entity = new BillPugh();
                    Boolean needRun = true;
                    entity.setNeedRun(needRun);
                    entityManager.persist(entity);
                    entityManager.flush();
                    // when
                    BillPugh found = repository.findAllByNeedRun(needRun);
                    // then
                    assertThat(found.getNeedRun()).isEqualTo(entity.getNeedRun());
                }
            }
        """
    }

    @Override
    TestGenerator generator() {
        new SpringRepositoryTestGenerator(solver, reporter, visitReporter, nomenclatureFactory, valueFactory, cfg)
    }
}
