/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.cache.Cache;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the Care Pathways implementation of the {@link Vocabulary}, {@link CarePathwaysOntology}.
 */
public class CarePathwaysOntologyTest
{
    private static final String IDENTIFIER = "care-pathways";

    private static final String NAME = "Care pathways tests and patient care";

    private static final String TERM_PREFIX = "CP";

    private static final String COLON = ":";

    private static final String TEST_CATEGORY = "test";

    private static final String CARE_CATEGORY = "care";

    private static final String ID_LABEL = "id";

    private static final String NAME_LABEL = "name";

    private static final String TERM_ID = "198";

    private static final String TERM_NAME = "term1_name";

    @Rule
    public final MockitoComponentMockingRule<Vocabulary> mocker =
        new MockitoComponentMockingRule<>(CarePathwaysOntology.class);

    @Mock
    private SolrClient solrClient;

    @Mock
    private Cache<VocabularyTerm> cache;

    @Mock
    private QueryResponse response;

    @Mock
    private SolrDocumentList termList;

    @Mock
    private SolrDocument termDoc;

    @Mock
    private VocabularyTerm term;

    private CarePathwaysOntology component;

    private Logger logger;

    @Before
    public void setUp() throws ComponentLookupException, IOException, SolrServerException
    {
        MockitoAnnotations.initMocks(this);
        this.component = (CarePathwaysOntology) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        final SolrVocabularyResourceManager externalServicesAccess =
            this.mocker.getInstance(SolrVocabularyResourceManager.class);
        when(externalServicesAccess.getTermCache(this.component)).thenReturn(this.cache);
        when(externalServicesAccess.getReplacementSolrConnection(this.component)).thenReturn(this.solrClient);
        when(externalServicesAccess.getSolrConnection(this.component)).thenReturn(this.solrClient);

        when(this.solrClient.query(any(SolrQuery.class))).thenReturn(this.response);
        when(this.response.getResults()).thenReturn(this.termList);
        when(this.termList.get(0)).thenReturn(this.termDoc);
        when(this.termDoc.getFieldValues(ID_LABEL)).thenReturn(Collections.singletonList(TERM_ID));
        when(this.termDoc.getFieldValues(NAME_LABEL)).thenReturn(Collections.singletonList(TERM_NAME));
    }

    @Test
    public void testCarePathwaysOntologyReindex() throws IOException, SolrServerException
    {
        final int ontologyServiceResult = this.component.reindex(this.getClass().getResource("/care-pathways-test.tsv")
            .toString());
        Mockito.verify(this.solrClient).commit();
        Mockito.verify(this.solrClient).add(Matchers.anyCollectionOf(SolrInputDocument.class));
        Mockito.verify(this.cache).removeAll();
        Mockito.verifyNoMoreInteractions(this.cache, this.solrClient);
        Assert.assertTrue(ontologyServiceResult == 0);
    }

    @Test
    public void getIdentifierReturnsCorrectVocabularyIdentifier()
    {
        Assert.assertTrue(IDENTIFIER.equals(this.component.getIdentifier()));
    }

    @Test
    public void getNameReturnsCorrectVocabularyName()
    {
        Assert.assertTrue(NAME.equals(this.component.getName()));
    }

    @Test
    public void getAliasesReturnsCorrectVocabularyAliases()
    {
        final Set<String> aliases = new HashSet<>();
        aliases.add(IDENTIFIER);
        aliases.add(TERM_PREFIX);
        Assert.assertEquals(aliases, this.component.getAliases());
    }

    @Test
    public void getSupportedCategoriesReturnsCorrectSupportedCategories()
    {
        Assert.assertEquals(Arrays.asList(TEST_CATEGORY, CARE_CATEGORY), this.component.getSupportedCategories());
    }

    @Test
    public void getDefaultSourceLocation()
    {
        final URL source =
            ClassLoader.getSystemClassLoader().getResource("source/CarePathwaysOrderedTestsAndCare.tsv");
        final String sourceStr = source == null ? StringUtils.EMPTY : source.toString();
        Assert.assertTrue(sourceStr.equals(this.component.getDefaultSourceLocation()));
    }

    @Test
    public void getWebsiteIsBlank()
    {
        Assert.assertTrue(StringUtils.EMPTY.equals(this.component.getWebsite()));
    }

    @Test
    public void getCitationIsBlank()
    {
        Assert.assertTrue(StringUtils.EMPTY.equals(this.component.getCitation()));
    }

    @Test
    public void getSolrDocsPerBatchReturnsCorrectNumber()
    {
        Assert.assertEquals(500000, this.component.getSolrDocsPerBatch());
    }

    @Test
    public void getTermReturnsNullWhenIdIsBlank()
    {
        Assert.assertNull(this.component.getTerm(null));
        Assert.assertNull(this.component.getTerm(StringUtils.EMPTY));
        Assert.assertNull(this.component.getTerm(StringUtils.SPACE));
    }

    @Test
    public void getTermReturnsNullIfNothingIsFound()
    {
        when(this.response.getResults()).thenReturn(null);
        Assert.assertNull(this.component.getTerm(TERM_ID));
    }

    @Test
    public void getTermReturnsTermIfIdIsValidAndInCache()
    {
        when(this.cache.get(TERM_ID)).thenReturn(this.term);
        Assert.assertEquals(this.term, this.component.getTerm(TERM_ID));
        verify(this.cache, times(1)).get(TERM_ID);
        verify(this.cache, never()).set(eq(TERM_ID), any(VocabularyTerm.class));
    }

    @Test
    public void getTermReturnsTermIfIdIsValidAndIsNotInCache()
    {
        when(this.cache.get(TERM_ID)).thenReturn(null);
        final VocabularyTerm result = this.component.getTerm(TERM_ID);
        Assert.assertNotNull(result);
        Assert.assertEquals(TERM_ID, result.getId());
        Assert.assertEquals(TERM_NAME, result.getName());
        verify(this.cache, times(1)).get(TERM_ID);
        verify(this.cache, times(1)).set(TERM_ID, result);
    }

    @Test
    public void loadWorksAsExpectedWithCorrectData() throws MalformedURLException
    {
        final URL url = new URL(this.getClass().getResource("/care-pathways-test.tsv").toString());
        final Collection<SolrInputDocument> docs = this.component.load(url);
        Assert.assertNotNull(docs);
        // 9 input terms plus the version
        Assert.assertEquals(10, docs.size());
        verifyZeroInteractions(this.logger);
    }

    @Test
    public void loadAddsAVersionTerm() throws MalformedURLException
    {
        final URL url = new URL(this.getClass().getResource("/care-pathways-test.tsv").toString());
        final Collection<SolrInputDocument> docs = this.component.load(url);
        Assert.assertNotNull(docs);
        Assert.assertEquals(10, docs.size());
        List<SolrInputDocument> versionDocs =
            docs.stream().filter(doc -> doc.getFieldValue("version") != null).collect(Collectors.toList());
        Assert.assertEquals(1, versionDocs.size());
        SolrInputDocument versionDoc = versionDocs.get(0);
        Assert.assertEquals("HEADER_INFO", versionDoc.getFieldValue("id"));
        DateTime version =
            ISODateTimeFormat.dateTime().withZoneUTC().parseDateTime((String) versionDoc.getFieldValue("version"));
        Assert.assertTrue(DateTime.now().isAfter(version));
        // We allow two minutes time for running tests on a busy slow machine
        Assert.assertTrue(Minutes.minutesBetween(version, DateTime.now()).getMinutes() < 2);
    }

    @Test
    public void loadReturnsNullWithMissingIdData() throws MalformedURLException
    {
        final URL url = new URL(this.getClass().getResource("/care-pathways-missing-id-test.tsv").toString());
        final Collection<SolrInputDocument> docs = this.component.load(url);
        Assert.assertNull(docs);
        verify(this.logger, times(1)).warn("Failed to read/parse the Care Pathways source: {}",
            "Term Molecular is missing an id");
    }

    @Test
    public void loadReturnsNullWithMissingNameData() throws MalformedURLException
    {
        final URL url = new URL(this.getClass().getResource("/care-pathways-missing-name-test.tsv").toString());
        final Collection<SolrInputDocument> docs = this.component.load(url);
        Assert.assertNull(docs);
        verify(this.logger, times(1)).warn("Failed to read/parse the Care Pathways source: {}",
            "Term [CP:NO_NAME] is missing a name");
    }

    @Test
    public void searchWithBlankInputReturnsEmpty()
    {
        Assert.assertTrue(this.component.search(StringUtils.EMPTY, 0, null, null).isEmpty());
    }

    @Test
    public void searchWithInputIsIdResultsInCorrectQuery() throws IOException, SolrServerException
    {
        when(this.response.getSpellCheckResponse()).thenReturn(null);
        when(this.response.getResults()).thenReturn(new SolrDocumentList());

        this.component.search(TERM_PREFIX + COLON + TERM_ID, 0, null, null);
        verify(this.solrClient).query(argThat(new IsIdQuery()));
    }

    @Test
    public void searchWithInputIsNotIdResultsInCorrectQuery() throws IOException, SolrServerException
    {
        when(this.response.getSpellCheckResponse()).thenReturn(null);
        when(this.response.getResults()).thenReturn(new SolrDocumentList());

        this.component.search(TERM_ID, 0, null, null);
        verify(this.solrClient).query(argThat(new IsDisMaxQuery()));
    }

    @Test
    public void searchWithMultiWordInputResultsInCorrectQuery() throws IOException, SolrServerException
    {
        when(this.response.getSpellCheckResponse()).thenReturn(null);
        when(this.response.getResults()).thenReturn(new SolrDocumentList());

        this.component.search(TERM_ID + StringUtils.SPACE + TERM_ID, 0, null, null);
        verify(this.solrClient).query(argThat(new IsDisMaxQuery()));
    }

    class IsIdQuery extends ArgumentMatcher<SolrParams>
    {
        @Override
        public boolean matches(Object argument)
        {
            SolrParams params = (SolrParams) argument;
            return params.get(CommonParams.FQ).startsWith("id")
                && params.get(DisMaxParams.PF) == null
                && params.get(DisMaxParams.QF) == null;
        }
    }

    class IsDisMaxQuery extends ArgumentMatcher<SolrParams>
    {
        @Override
        public boolean matches(Object argument)
        {
            SolrParams params = (SolrParams) argument;
            return params.get(DisMaxParams.PF) != null
                && params.get(DisMaxParams.QF) != null
                && params.get(CommonParams.Q) != null;
        }
    }
}
