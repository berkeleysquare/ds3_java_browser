/*
 * ****************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.models.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PriorityFilter {
    public static Priority[] filterPriorities(final Priority[] priorities) {
        final Priority[] elements = {Priority.BACKGROUND, Priority.CRITICAL};
        final List<Priority> list = new ArrayList<Priority>(Arrays.asList(priorities));
        list.removeAll(Arrays.asList(elements));
        return list.toArray(new Priority[0]);
    }

}
