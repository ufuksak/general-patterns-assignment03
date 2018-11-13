package com.aurea.testgenerator.generation.names.pojos

import com.aurea.testgenerator.generation.names.TestClassNomenclature
import com.aurea.testgenerator.generation.names.TestClassNomenclatureFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


@Component
@Profile(['pojo-tester', 'open-pojo-zz'])
class PojoTestClassNomenclatureFactory implements TestClassNomenclatureFactory {
    @Override
    TestClassNomenclature newTestClassNomenclature() {
        new PojoTestClassNomenclature()
    }
}
