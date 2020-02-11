/* Copyright (C) 2013-2020 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.automatalib.automata.words.basic;

import java.util.List;

import net.automatalib.automata.words.util.GrowingVPDAlphabetTestUtil;
import net.automatalib.words.impl.GrowingVPDAlphabet;
import net.automatalib.words.impl.VPDSym;

/**
 * @author frohme
 */
public class GrowingVPDAlphabetTest extends AbstractAlphabetTest<VPDSym<Character>, GrowingVPDAlphabet<Character>> {

    @Override
    protected List<VPDSym<Character>> getAlphabetSymbols() {
        return GrowingVPDAlphabetTestUtil.JOINED_SYMBOLS;
    }

    @Override
    protected List<VPDSym<Character>> getNonAlphabetSymbols() {
        return GrowingVPDAlphabetTestUtil.NON_CONTAINED_SYMBOLS;
    }

    @Override
    protected GrowingVPDAlphabet<Character> getAlphabet() {
        return GrowingVPDAlphabetTestUtil.ALPHABET;
    }
}
