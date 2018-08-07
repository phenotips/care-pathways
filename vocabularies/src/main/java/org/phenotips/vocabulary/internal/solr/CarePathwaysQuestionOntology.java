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

import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Provides access to the Care Pathways questions vocabulary. The vocabulary prefix is {@code CPQ}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("care-pathways-questions")
@Singleton
public class CarePathwaysQuestionOntology extends AbstractCSVSolrVocabulary
{
    /** The category for care pathways question-related terms. */
    private static final String QUESTION_CATEGORY = "survey-question";

    /** The standard prefix for the vocabulary terms. */
    private static final String STANDARD_PREFIX = "CPQ";

    /** The label for the field storing ID for term. */
    private static final String ID = "id";

    /** The label for the field storing the question string. */
    private static final String NAME = "name";

    /** The label for the field storing the question group. */
    private static final String TERM_GROUP = "term_group";

    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^CPQ:[0-9]+$", Pattern.CASE_INSENSITIVE);

    @Override
    protected int getSolrDocsPerBatch()
    {
        return 500000;
    }

    @Override
    public String getIdentifier()
    {
        return "care-pathways-questions";
    }

    @Override
    public String getName()
    {
        return "Care pathways survey questions";
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> aliases = new HashSet<>();
        aliases.add(getIdentifier());
        aliases.add(STANDARD_PREFIX);
        return aliases;
    }

    @Override
    public Collection<String> getSupportedCategories()
    {
        return Collections.singletonList(QUESTION_CATEGORY);
    }

    @Override
    public String getDefaultSourceLocation()
    {
        final URL url = getClass().getResource("/source/CarePathwaysQuestions.tsv");
        return url == null ? StringUtils.EMPTY : url.toString();
    }

    @Override
    public String getWebsite()
    {
        return StringUtils.EMPTY;
    }

    @Override
    public String getCitation()
    {
        return StringUtils.EMPTY;
    }

    @Override
    protected Collection<SolrInputDocument> load(final URL url)
    {
        // Try to read from the input file.
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(getInputStream(url), StandardCharsets.UTF_8))) {
            final CSVFormat parser = CSVFormat.TDF;
            final CSVParser parsed = parser.parse(in);
            // Collect the solr documents.
            Collection<SolrInputDocument> result = StreamSupport.stream(parsed.spliterator(), false)
                .map(this::buildSolrDocForQuestion)
                .collect(Collectors.toList());
            // Add a "version" term with the version set to the current datetime
            result.add(new SolrInputDocument("id", "HEADER_INFO", "version",
                ISODateTimeFormat.dateTime().withZoneUTC().print(DateTime.now())));
            return result;
        } catch (final Exception ex) {
            this.logger.warn("Failed to read/parse the Care Pathways question source: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public List<VocabularyTerm> search(
        @Nullable String input,
        int maxResults,
        @Nullable String sort,
        @Nullable String customFilter)
    {
        // Only one category.
        return search(input, QUESTION_CATEGORY, maxResults, sort, customFilter);
    }

    @Override
    public List<VocabularyTerm> search(
        @Nullable final String input,
        @Nonnull final String category,
        final int maxResults,
        @Nullable final String sort,
        @Nullable final String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        // If the wrong category was provided for the vocabulary, want to provide an appropriate log message.
        if (!getSupportedCategories().contains(category)) {
            this.logger.warn("The provided category [{}] is not supported by the Care Pathways questions vocabulary.",
                category);
            return Collections.emptyList();
        }
        return search(input, maxResults, sort, customFilter, isId(input));
    }

    /**
     * Add query parameters and perform search.
     *
     * @param input the submitted input
     * @param maxResults the maximum number of results to return
     * @param sort the sorting order
     * @param filter filters to apply to query
     * @param isId true iff {@code input} is an ID
     * @return a list of {@link VocabularyTerm results} matching {@code input}
     */
    private List<VocabularyTerm> search(
        @Nonnull final String input,
        final int maxResults,
        @Nullable final String sort,
        @Nullable final String filter,
        final boolean isId)
    {
        final SolrQuery query = new SolrQuery();
        // Add field query parameters if input is not an ID, then add global parameters.
        addGlobalQueryParameters(!isId ? addFieldQueryParameters(query) : query);
        // Add dynamic query parameters, and search.
        return search(addDynamicQueryParameters(input, maxResults, sort, filter, isId, query)).stream()
            .map(doc -> new SolrVocabularyTerm(doc, this))
            .collect(Collectors.toList());
    }

    /**
     * Add the global parameters to the {@code query}.
     *
     * @param query the {@link SolrQuery} being formed
     * @return the updated {@link SolrQuery}
     */
    private SolrQuery addGlobalQueryParameters(final SolrQuery query)
    {
        query.set("spellcheck", Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COUNT, "100");
        query.set(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        query.set("lowercaseOperators", Boolean.toString(false));
        query.set("defType", "edismax");
        return query;
    }

    /**
     * Add the field parameters to the {@code query}.
     *
     * @param query the {@link SolrQuery} being formed
     * @return the updated {@link SolrQuery}
     */
    private SolrQuery addFieldQueryParameters(final SolrQuery query)
    {
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 text^3 textSpell^5");
        query.set(DisMaxParams.QF, "name^10 nameSpell^18 nameStub^5 text^1 textSpell^2 textStub^0.5");
        return query;
    }

    /**
     * Add the dynamic parameters to the {@code query}.
     *
     * @param originalQuery the original query string submitted
     * @param rows the number of results to return
     * @param sort the sorting order
     * @param customFq filters to apply to query
     * @param isId true iff {@code originalQuery} is an ID, false otherwise
     * @param query the {@link SolrQuery} being formed
     * @return the updated {@link SolrQuery}
     */
    @Nonnull
    private SolrQuery addDynamicQueryParameters(
        @Nonnull final String originalQuery,
        @Nonnull final Integer rows,
        @Nullable final String sort,
        @Nullable final String customFq,
        final boolean isId,
        @Nonnull final SolrQuery query)
    {
        final String queryString = originalQuery.trim();
        final String escapedQuery = ClientUtils.escapeQueryChars(queryString);
        if (isId) {
            query.setFilterQueries(StringUtils.defaultIfBlank(customFq,
                new MessageFormat("id:{0}").format(new String[] { escapedQuery })));
        } else if (StringUtils.isNotBlank(customFq)) {
            query.setFilterQueries(customFq);
        }
        query.setQuery(escapedQuery);
        query.set(SpellingParams.SPELLCHECK_Q, queryString);
        query.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            for (final String sortItem : sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, " "),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-") ? ORDER.desc : ORDER.asc);
            }
        }
        return query;
    }

    /**
     * Returns true iff {@code input} is an identifier.
     *
     * @param input the query string
     * @return true iff the {@code input} is an identifier
     */
    private boolean isId(@Nonnull final String input)
    {
        return ID_PATTERN.matcher(input).matches();
    }

    /**
     * Creates and returns a {@link SolrInputDocument} for the provided {@code questionRow}.
     *
     * @param questionRow the {@link CSVRecord} containing the data for one question
     * @return the {@link SolrInputDocument} containing data from {@code questionRow}
     */
    @Nonnull
    private SolrInputDocument buildSolrDocForQuestion(@Nonnull final CSVRecord questionRow)
    {
        final int rowSize = questionRow.size();
        // Add the data.
        if (rowSize >= 3 && StringUtils.isNoneBlank(questionRow.get(0), questionRow.get(1), questionRow.get(2))) {
            final SolrInputDocument doc = new SolrInputDocument();
            doc.addField(TERM_GROUP, questionRow.get(0).trim());
            doc.addField(NAME, questionRow.get(1).trim());
            doc.addField(ID, questionRow.get(2).trim());
            return doc;
        } else {
            throw new IllegalArgumentException("One of the required data fields is blank for record "
                + questionRow);
        }
    }

    /**
     * Gets the {@link InputStream} from the provided {@code url}.
     *
     * @param url the {@link URL} containing the data
     * @return the {@link InputStream}
     * @throws IOException on failure
     */
    @Nonnull
    private InputStream getInputStream(@Nonnull final URL url) throws IOException
    {
        return url.openConnection().getInputStream();
    }
}
