/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mpt.imapmailbox.maildir.host;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.maildir.MaildirMailboxManagerProvider;
import org.apache.james.mailbox.store.MailboxManagerOptions;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.quota.DefaultQuotaRootResolver;
import org.apache.james.mailbox.store.quota.NoQuotaManager;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.mpt.api.ImapFeatures;
import org.apache.james.mpt.api.ImapFeatures.Feature;
import org.apache.james.mpt.host.JamesImapHostSystem;
import org.junit.rules.TemporaryFolder;

public class MaildirHostSystem extends JamesImapHostSystem {

    public static final String META_DATA_DIRECTORY = "target/user-meta-data";
    private static final String MAILDIR_HOME = "target/Maildir";
    private static final ImapFeatures SUPPORTED_FEATURES = ImapFeatures.of();
    
    private final TemporaryFolder temporaryFolder;
    private StoreMailboxManager mailboxManager;

    public MaildirHostSystem(TemporaryFolder temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
    }


    @Override
    public void beforeTest() throws Exception {
        super.beforeTest();
        mailboxManager = MaildirMailboxManagerProvider
            .createMailboxManager(MAILDIR_HOME + "/%user", temporaryFolder,
                MailboxManagerOptions.builder()
                    .withAuthorizator(authorizator)
                    .withAuthenticator(authenticator)
                    .build());
        StoreSubscriptionManager sm = new StoreSubscriptionManager(mailboxManager.getMapperFactory());

        ImapProcessor defaultImapProcessorFactory =
                DefaultImapProcessorFactory.createDefaultProcessor(
                        mailboxManager,
                        sm, 
                        new NoQuotaManager(), 
                        new DefaultQuotaRootResolver(mailboxManager.getMapperFactory()),
                        new DefaultMetricFactory());
        configure(new DefaultImapDecoderFactory().buildImapDecoder(),
                new DefaultImapEncoderFactory().buildImapEncoder(),
                defaultImapProcessorFactory);
        (new File(MAILDIR_HOME)).mkdirs();
    }


    @Override
    public void afterTest() throws Exception {
        resetUserMetaData();
        try {
        	FileUtils.deleteDirectory(new File(MAILDIR_HOME));
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    public void resetUserMetaData() throws Exception {
        File dir = new File(META_DATA_DIRECTORY);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }

    @Override
    protected MailboxManager getMailboxManager() {
        return mailboxManager;
    }

    @Override
    public boolean supports(Feature... features) {
        return SUPPORTED_FEATURES.supports(features);
    }

    @Override
    public void setQuotaLimits(long maxMessageQuota, long maxStorageQuota) throws Exception {
        throw new NotImplementedException();
    }

}
