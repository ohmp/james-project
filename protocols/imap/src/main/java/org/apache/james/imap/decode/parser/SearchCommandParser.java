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
package org.apache.james.imap.decode.parser;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.james.imap.api.ImapCommand;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.Tag;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.message.IdRange;
import org.apache.james.imap.api.message.UidRange;
import org.apache.james.imap.api.message.request.DayMonthYear;
import org.apache.james.imap.api.message.request.SearchKey;
import org.apache.james.imap.api.message.request.SearchOperation;
import org.apache.james.imap.api.message.request.SearchResultOption;
import org.apache.james.imap.api.message.response.StatusResponse;
import org.apache.james.imap.api.message.response.StatusResponse.ResponseCode;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.imap.message.request.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse SEARCH commands
 */
public class SearchCommandParser extends AbstractUidCommandParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCommandParser.class);

    public SearchCommandParser(StatusResponseFactory statusResponseFactory) {
        super(ImapCommand.selectedStateCommand(ImapConstants.SEARCH_COMMAND_NAME), statusResponseFactory);
    }

    /**
     * Parses the request argument into a valid search term.
     * 
     * @param request
     *            <code>ImapRequestLineReader</code>, not null
     * @param charset
     *            <code>Charset</code> or null if there is no charset
     * @param isFirstToken
     *            true when this is the first token read, false otherwise
     */
    protected SearchKey searchKey(ImapSession session, ImapRequestLineReader request, Charset charset, boolean isFirstToken) throws DecodingException, IllegalCharsetNameException, UnsupportedCharsetException {
        final char next = request.nextChar();
        
        if (next >= '0' && next <= '9' || next == '*' || next == '$') {
            return sequenceSet(session, request);
        } else if (next == '(') {
            return paren(session, request, charset);
        } else {
            return parseSearchKey(session, request, charset, isFirstToken);
        }
    }

    private SearchKey parseSearchKey(ImapSession session, ImapRequestLineReader request, Charset charset, boolean isFirstToken) throws DecodingException {
        String searchKey = request.atom().toUpperCase(Locale.US);
        switch (searchKey) {
            case "ANSWERED":
                return SearchKey.buildAnswered();
            case "ALL":
                return SearchKey.buildAll();
            case "BCC":
                return bcc(request, charset);
            case "BEFORE":
                return before(request);
            case "BODY":
                return body(request, charset);
            case "CC":
                return cc(request, charset);
            case "CHARSET":
                return charset(session, request, isFirstToken);
            case "DELETED":
                return SearchKey.buildDeleted();
            case "DRAFT":
                return SearchKey.buildDraft();
            case "FLAGGED":
                return SearchKey.buildFlagged();
            case "FROM":
                return from(request, charset);
            case "HEADER":
                return header(request, charset);
            case "KEYWORD":
                return keyword(request);
            case "LARGER":
                return larger(request);
            case "MODSEQ":
                return modseq(request);
            case "NEW":
                return SearchKey.buildNew();
            case "NOT":
                return not(session, request, charset);
            case "OLD":
                return SearchKey.buildOld();
            case "OLDER":
                return older(request);
            case "ON":
                return on(request);
            case "OR":
                return or(session, request, charset);
            case "RECENT":
                return SearchKey.buildRecent();
            case "SEEN":
                return SearchKey.buildSeen();
            case "SENTBEFORE":
                return sentBefore(request);
            case "SENTON":
                return sentOn(request);
            case "SENTSINCE":
                return sentSince(request);
            case "SINCE":
                return since(request);
            case "SMALLER":
                return smaller(request);
            case "SUBJECT":
                return subject(request, charset);
            case "TEXT":
                return text(request, charset);
            case "TO":
                return to(request, charset);
            case "UID":
                return uid(request);
            case "UNANSWERED":
                return SearchKey.buildUnanswered();
            case "UNDELETED":
                return SearchKey.buildUndeleted();
            case "UNDRAFT":
                return SearchKey.buildUndraft();
            case "UNFLAGGED":
                return SearchKey.buildUnflagged();
            case "UNKEYWORD":
                return unkeyword(request);
            case "UNSEEN":
                return SearchKey.buildUnseen();
            case "YOUNGER":
                return younger(request);
            default:
                throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    private SearchKey modseq(ImapRequestLineReader request) throws DecodingException {
        try {
            return SearchKey.buildModSeq(request.number());
        } catch (DecodingException e) {
            // Just consume the [<entry-name> <entry-type-req>] and ignore it
            // See RFC4551 3.4. MODSEQ Search Criterion in SEARCH
            request.consumeQuoted();
            request.consumeWord(chr -> true);
            return SearchKey.buildModSeq(request.number());
        }
    }

    private SearchKey paren(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        request.consume();
        List<SearchKey> keys = new ArrayList<>();
        addUntilParen(session, request, keys, charset);
        return SearchKey.buildAnd(keys);
    }

    private void addUntilParen(ImapSession session, ImapRequestLineReader request, List<SearchKey> keys, Charset charset) throws DecodingException {
        final char next = request.nextWordChar();
        if (next == ')') {
            request.consume();
        } else {
            final SearchKey key = searchKey(session, request, null, false);
            keys.add(key);
            addUntilParen(session, request, keys, charset);
        }
    }

    private int consumeAndCap(ImapRequestLineReader request) throws DecodingException {
        final char next = request.consume();
        return ImapRequestLineReader.cap(next);
    }
    
    private SearchKey cc(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        String value = request.astring(charset);
        return SearchKey.buildCc(value);
    }

    private SearchKey charset(ImapSession session, ImapRequestLineReader request, boolean isFirstToken) throws DecodingException, IllegalCharsetNameException, UnsupportedCharsetException {
        nextIsSpace(request);
        if (!isFirstToken) {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
        final String value = request.astring();
        final Charset charset = Charset.forName(value);
        request.nextWordChar();
        return searchKey(session, request, charset, false);
    }

    private SearchKey keyword(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        String value = request.atom();
        return SearchKey.buildKeyword(value);
    }

    private SearchKey unkeyword(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        String value = request.atom();
        return SearchKey.buildUnkeyword(value);
    }

    private SearchKey header(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        String field = request.astring(charset);
        nextIsSpace(request);
        String value = request.astring(charset);
        return SearchKey.buildHeader(field, value);
    }

    private SearchKey larger(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        long value = request.number();
        return SearchKey.buildLarger(value);
    }

    private SearchKey smaller(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        long value = request.number();
        return SearchKey.buildSmaller(value);
    }

    private SearchKey from(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        String value = request.astring(charset);
        return SearchKey.buildFrom(value);
    }
    
    private SearchKey younger(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        return SearchKey.buildYounger(request.nzNumber());
    }
    
    private SearchKey older(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        return SearchKey.buildOlder(request.nzNumber());
    }

    private SearchKey or(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        final SearchKey result;
        nextIsSpace(request);
        final SearchKey firstKey = searchKey(session, request, charset, false);
        nextIsSpace(request);
        final SearchKey secondKey = searchKey(session, request, charset, false);
        result = SearchKey.buildOr(firstKey, secondKey);
        return result;
    }

    private SearchKey not(ImapSession session, ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        SearchKey nextKey = searchKey(session, request, charset, false);
        return SearchKey.buildNot(nextKey);
    }

    private SearchKey body(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        String value = request.astring(charset);
        return SearchKey.buildBody(value);
    }

    private SearchKey on(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        DayMonthYear value = request.date();
        return SearchKey.buildOn(value);
    }

    private SearchKey sentBefore(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        DayMonthYear value = request.date();
        return SearchKey.buildSentBefore(value);
    }

    private SearchKey sentSince(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        DayMonthYear value = request.date();
        return SearchKey.buildSentSince(value);
    }

    private SearchKey since(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        DayMonthYear value = request.date();
        return SearchKey.buildSince(value);
    }

    private SearchKey sentOn(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        DayMonthYear value = request.date();
        return SearchKey.buildSentOn(value);
    }

    private SearchKey before(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        DayMonthYear value = request.date();
        return SearchKey.buildBefore(value);
    }

    private SearchKey bcc(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        String value = request.astring(charset);
        return SearchKey.buildBcc(value);
    }

    private SearchKey text(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        String value = request.astring(charset);
        return SearchKey.buildText(value);
    }

    private SearchKey uid(ImapRequestLineReader request) throws DecodingException {
        nextIsSpace(request);
        UidRange[] range = request.parseUidRange();
        return SearchKey.buildUidSet(range);
    }

    private SearchKey sequenceSet(ImapSession session, ImapRequestLineReader request) throws DecodingException {
        final IdRange[] range = request.parseIdRange(session);
        return SearchKey.buildSequenceSet(range);
    }

    private SearchKey to(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        String value = request.astring(charset);
        return SearchKey.buildTo(value);
    }

    private SearchKey subject(ImapRequestLineReader request, Charset charset) throws DecodingException {
        nextIsSpace(request);
        final String value = request.astring(charset);
        return SearchKey.buildSubject(value);
    }

    private void nextIsSpace(ImapRequestLineReader request) throws DecodingException {
        final char next = request.consume();
        if (next != ' ') {
            throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
        }
    }

    public SearchKey decode(ImapSession session, ImapRequestLineReader request) throws DecodingException, IllegalCharsetNameException, UnsupportedCharsetException {
        request.nextWordChar();
        final SearchKey firstKey = searchKey(session, request, null, true);
        final SearchKey result;
        if (request.nextChar() == ' ') {
            List<SearchKey> keys = new ArrayList<>();
            keys.add(firstKey);
            while (request.nextChar() == ' ') {
                request.nextWordChar();
                final SearchKey key = searchKey(session, request, null, false);
                keys.add(key);
            }
            result = SearchKey.buildAnd(keys);
        } else {
            result = firstKey;
        }
        request.eol();
        return result;
    }

    private ImapMessage unsupportedCharset(Tag tag, ImapCommand command) {
        final ResponseCode badCharset = StatusResponse.ResponseCode.badCharset();
        return taggedNo(tag, command, HumanReadableText.BAD_CHARSET, badCharset);
    }

    /**
     * Parse the {@link SearchResultOption}'s which are used for ESEARCH
     */
    private List<SearchResultOption> parseOptions(ImapRequestLineReader reader) throws DecodingException {
        List<SearchResultOption> options = new ArrayList<>();
        reader.consumeChar('(');
        reader.nextWordChar();

        while (reader.nextChar() != ')') {
            String option = reader.atom();
            switch (option) {
                case "ALL":
                    options.add(SearchResultOption.ALL);
                    break;
                case "COUNT":
                    options.add(SearchResultOption.COUNT);
                    break;
                case "MAX":
                    options.add(SearchResultOption.MAX);
                    break;
                case "MIN":
                    options.add(SearchResultOption.MIN);
                    break;
                // Check for SAVE options which is part of the SEARCHRES extension
                case "SAVE":
                    options.add(SearchResultOption.SAVE);
                    break;
                default:
                    throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
            }
            reader.nextWordChar();
        }
        // if the options are empty then we parsed RETURN () which is a shortcut for ALL.
        // See http://www.faqs.org/rfcs/rfc4731.html 3.1
        if (options.isEmpty()) {
            options.add(SearchResultOption.ALL);
        }
        return options;
    }
    
    @Override
    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, Tag tag, boolean useUids, ImapSession session) throws DecodingException {
        try {
            SearchKey recent = null;
            List<SearchResultOption> options = null;
            int c = ImapRequestLineReader.cap(request.nextWordChar());
            if (c == 'R') {
                // if we found a R its either RECENT or RETURN so consume it
                String atom = request.atom().toUpperCase(Locale.US);

                switch (atom) {
                    case "RECENT":
                        recent = SearchKey.buildRecent();
                        break;
                    case "RETURN":
                        request.nextWordChar();
                        options = parseOptions(request);
                        break;
                    default:
                        throw new DecodingException(HumanReadableText.ILLEGAL_ARGUMENTS, "Unknown search key");
                }
            }
            final SearchKey finalKey;

            if (recent != null) {
                if (request.nextChar() != ' ') {
                    request.eol();
                    finalKey = recent;
                } else {
                    // Parse the search term from the request
                    final SearchKey key = decode(session, request);
                    finalKey = SearchKey.buildAnd(Arrays.asList(recent, key));
                }
            } else {
                // Parse the search term from the request
                finalKey = decode(session, request);
            }
            
            if (options == null) {
                options = new ArrayList<>();
            }

            return new SearchRequest(command, new SearchOperation(finalKey, options), useUids, tag);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            LOGGER.debug("Unable to decode request", e);
            return unsupportedCharset(tag, command);
        }
    }

}
