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
package org.apache.james.mpt.imapmailbox.jcr.host;

import java.io.File;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.mailbox.NamespaceReservedMailboxMatcher;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jcr.JCRMailboxManager;
import org.apache.james.mailbox.jcr.JCRMailboxManagerProvider;
import org.apache.james.mailbox.jcr.JCRSubscriptionManager;
import org.apache.james.mailbox.store.MailboxManagerOptions;
import org.apache.james.mailbox.store.quota.DefaultQuotaRootResolver;
import org.apache.james.mailbox.store.quota.NoQuotaManager;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.mpt.api.ImapFeatures;
import org.apache.james.mpt.api.ImapFeatures.Feature;
import org.apache.james.mpt.host.JamesImapHostSystem;
import org.apache.james.protocols.imap.DefaultNamespaceConfiguration;
import org.xml.sax.InputSource;

public class JCRHostSystem extends JamesImapHostSystem {

    public static JamesImapHostSystem build() throws Exception {
        return new JCRHostSystem();
    }
    
    private final JCRMailboxManager mailboxManager;

    private static final String JACKRABBIT_HOME = "target/jackrabbit";
    public static final String META_DATA_DIRECTORY = "target/user-meta-data";
    private static final ImapFeatures SUPPORTED_FEATURES = ImapFeatures.of(Feature.NAMESPACE_SUPPORT);

    private Optional<RepositoryImpl> repository;
    
    public JCRHostSystem() throws Exception {

        delete(new File(JACKRABBIT_HOME));
        
        try {

            String user = "user";
            String pass = "pass";
            String workspace = null;

            if (!repository.isPresent()) {
                repository = Optional.of(JCRMailboxManagerProvider.createRepository());
            }

            mailboxManager = JCRMailboxManagerProvider
                .provideMailboxManager(user, pass, workspace, repository.get(),
                    MailboxManagerOptions.builder()
                        .withAuthenticator(authenticator)
                        .withAuthorizator(authorizator)
                        .build());

            final ImapProcessor defaultImapProcessorFactory = 
                    DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager, 
                            new JCRSubscriptionManager(mailboxManager.getMapperFactory()),
                            new NoQuotaManager(), 
                            new DefaultQuotaRootResolver(mailboxManager.getMapperFactory()),
                            new DefaultMetricFactory());
            resetUserMetaData();
            MailboxSession session = mailboxManager.createSystemSession("test");
            mailboxManager.startProcessingRequest(session);
            //mailboxManager.deleteEverything(session);
            mailboxManager.endProcessingRequest(session);
            mailboxManager.logout(session, false);
            
            configure(new DefaultImapDecoderFactory().buildImapDecoder(), new DefaultImapEncoderFactory().buildImapEncoder(), defaultImapProcessorFactory);
        } catch (Exception e) {
            shutdownRepository();
            throw e;
        }
    }

    @Override
    public void afterTest() throws Exception {
        resetUserMetaData();
      
    }
    
    public void resetUserMetaData() throws Exception {
        File dir = new File(META_DATA_DIRECTORY);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
    }

    //JCR tests are broken partly because of that method not being run 
    public void afterTests() throws Exception {
        shutdownRepository();
    }
    
    private void shutdownRepository() throws Exception{
        repository.ifPresent(RepositoryImpl::shutdown);
        repository = Optional.empty();
    }
    
    private void delete(File home) throws Exception{
        if (home.exists()) {
            File[] files = home.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.isDirectory()) {
                    delete(f);
                } else {
                    f.delete();
                }
            }
            home.delete();
        }
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
