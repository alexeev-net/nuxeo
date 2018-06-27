/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk.actions;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.bulk.BulkCommand;
import org.nuxeo.ecm.core.bulk.BulkCommandHelper;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.2
 */
public class SetPropertyAction implements StreamProcessorTopology {

    private static final Log log = LogFactory.getLog(SetPropertyAction.class);

    public static final String COMPUTATION_NAME = "SetProperty";

    public static final String STREAM_NAME = "setProperty";

    @Override
    public Topology getTopology(Map<String, String> options) {

        return Topology.builder()
                       .addComputation( //
                               () -> new SetPropertyComputation(COMPUTATION_NAME),
                               Collections.singletonList("i1:" + STREAM_NAME))
                       .build();
    }

    public static class SetPropertyComputation extends AbstractComputation {

        public SetPropertyComputation(String name) {
            super(name, 1, 0);
        }

        @Override
        public void processRecord(ComputationContext context, String inputStreamName, Record record) {
            BulkCommand command = BulkCommandHelper.getBulkCommandJson(record.getData());
            String xpath = (String) command.getParams().get("xpath");
            Serializable value = command.getParams().get("value");
            String docId = record.getKey().split("/")[1];

            TransactionHelper.runInTransaction(() -> {
                try (CloseableCoreSession session = CoreInstance.openCoreSession(command.getRepository(),
                        command.getUsername())) {
                    DocumentModel doc = session.getDocument(new IdRef(docId));
                    doc.setPropertyValue(xpath, value);
                    session.saveDocument(doc);
                }
            });
            context.askForCheckpoint();
        }
    }
}
