/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.impexp;

import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.impexp.ExportImportAuditEntry;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.loadBaseModel;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ExportImportAuditServiceTest {
    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    ExportImportAuditService auditService;

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        loadBaseModel(typeDefStore, typeRegistry);
    }

    @Test
    public void checkTypeRegistered() throws AtlasBaseException {
        AtlasType auditEntryType = typeRegistry.getType("__" + ExportImportAuditEntry.class.getSimpleName());
        assertNotNull(auditEntryType);
    }

    @Test
    public void saveLogEntry() throws AtlasBaseException, InterruptedException {
        final String source1 = "clx";
        final String target1 = "cly";
        ExportImportAuditEntry entry = saveAndGet(source1, ExportImportAuditEntry.OPERATION_EXPORT, target1);

        String source2 = "clx2";
        String target2 = "clx1";
        ExportImportAuditEntry entry2 = saveAndGet(source2, ExportImportAuditEntry.OPERATION_EXPORT, target2);

        Thread.sleep(1000);
        ExportImportAuditEntry actualEntry = retrieveEntry(entry);
        ExportImportAuditEntry actualEntry2 = retrieveEntry(entry2);

        assertNotEquals(actualEntry.getGuid(), actualEntry2.getGuid());
        assertNotNull(actualEntry.getGuid());
        assertEquals(actualEntry.getSourceClusterName(), entry.getSourceClusterName());
        assertEquals(actualEntry.getTargetClusterName(), entry.getTargetClusterName());
        assertEquals(actualEntry.getOperation(), entry.getOperation());
    }

    @Test(enabled = false)
    public void numberOfSavedEntries_Retrieved() throws AtlasBaseException, InterruptedException {
        final String source1 = "cluster1";
        final String target1 = "cly";
        int MAX_ENTRIES = 5;

        for (int i = 0; i < MAX_ENTRIES; i++) {
            saveAndGet(source1, ExportImportAuditEntry.OPERATION_EXPORT, target1);
        }

        Thread.sleep(5000);
        AtlasSearchResult results = auditService.get(source1, ExportImportAuditEntry.OPERATION_EXPORT, "", "", "", "", 10, 0);
        assertEquals(results.getEntities().size(), MAX_ENTRIES);
    }


    private ExportImportAuditEntry retrieveEntry(ExportImportAuditEntry entry) throws AtlasBaseException, InterruptedException {
        Thread.sleep(5000);
        AtlasSearchResult result = auditService.get(entry.getUserName(), entry.getOperation(), entry.getSourceClusterName(),
                                                            entry.getTargetClusterName(), Long.toString(entry.getStartTime()), "", 10, 0);
        assertNotNull(result);
        assertEquals(result.getEntities().size(), 1);
        entry.setGuid(result.getEntities().get(0).getGuid());
        return auditService.get(entry);
    }

    private ExportImportAuditEntry saveAndGet(String sourceClusterName, String operation, String targetClusterName) throws AtlasBaseException {
        ExportImportAuditEntry entry = new ExportImportAuditEntry(sourceClusterName, operation);

        entry.setTargetClusterName(targetClusterName);
        entry.setUserName("default");
        entry.setStartTime(System.currentTimeMillis());
        entry.setEndTime(System.currentTimeMillis() + 1000L);
        auditService.save(entry);
        return entry;
    }
}
