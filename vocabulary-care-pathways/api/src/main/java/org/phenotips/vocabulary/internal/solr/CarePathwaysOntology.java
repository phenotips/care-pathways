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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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

/**
 * Provides access to the Care Pathways care and tests vocabulary. The vocabulary prefix is {@code CP}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("care-pathways")
@Singleton
public class CarePathwaysOntology extends AbstractCSVSolrVocabulary
{
    /** The category for care pathways test-related terms. */
    private static final String TEST_CATEGORY = "test";

    /** The category for care pathways care-related terms. */
    private static final String CARE_CATEGORY = "care";

    /** The label for the field storing ancestors for term. */
    private static final String TERM_CATEGORY = "term_category";

    /** The standard prefix for the vocabulary terms. */
    private static final String STANDARD_PREFIX = "CP";

    /** A colon. */
    private static final String COLON = ":";

    /** The filter for obtaining only the test-related terms. */
    private static final String DEFAULT_TEST_FILTER = TERM_CATEGORY + COLON + STANDARD_PREFIX + "\\:1";

    /** The filter for obtaining only the care-related terms. */
    private static final String DEFAULT_CARE_FILTER = TERM_CATEGORY + COLON + STANDARD_PREFIX + "\\:2";

    /** The label for the field storing ID for term. */
    private static final String ID = "id";

    /** The label for the field storing term name. */
    private static final String NAME = "name";

    /** The label for the field storing parents for term. */
    private static final String IS_A = "is_a";

    /** The open character. */
    private static final String OPEN = "[";

    /** The close character. */
    private static final String CLOSE = "]";

    /** The list of supported categories for this vocabulary. */
    private static final Collection<String> SUPPORTED_CATEGORIES = Arrays.asList(TEST_CATEGORY, CARE_CATEGORY);

    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^CP:[0-9]+$", Pattern.CASE_INSENSITIVE);

    @Override
    public String getIdentifier()
    {
        return "care-pathways";
    }

    @Override
    public String getName()
    {
        return "Care pathways tests and patient care";
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
        return SUPPORTED_CATEGORIES;
    }

    @Override
    public String getDefaultSourceLocation()
    {
        final URL url = ClassLoader.getSystemClassLoader().getResource("source/CarePathwaysOrderedTestsAndCare.tsv");
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

    @Nonnull
    private InputStream getInputStream(@Nonnull final URL url) throws IOException
    {
        return url.openConnection().getInputStream();
    }

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        return 500000;
    }

    @Nullable
    @Override
    public VocabularyTerm getTerm(@Nullable final String id)
    {
        final VocabularyTerm result = super.getTerm(id);
        if (result == null) {
            final String optionalPrefix = STANDARD_PREFIX + COLON;
            return StringUtils.startsWith(id, optionalPrefix)
                ? getTerm(StringUtils.substringAfter(id, optionalPrefix))
                : null;
        }
        return result;
    }

    @Override
    protected Collection<SolrInputDocument> load(@Nonnull final URL url)
    {
        // Try to read from the input file.
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(getInputStream(url), StandardCharsets.UTF_8))) {
            final CSVFormat parser = CSVFormat.TDF;
            final CSVParser parsed = parser.parse(in);
            // Collect the parent and ancestor information for all nodes.
            final Map<String, CPNode> nodes = StreamSupport.stream(parsed.spliterator(), false)
                .map(this::getPathData)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CPNode::getId, Function.identity(), this::mergeNodes));
            // Transfer the data to solr documents, and return.
            return nodes.values().stream()
                .map(this::buildSolrDoc)
                .collect(Collectors.toList());
        } catch (final Exception ex) {
            this.logger.warn("Failed to read/parse the Care Pathways source: {}", ex.getMessage());
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
        // Search in the test category by default.
        return search(input, TEST_CATEGORY, maxResults, sort, customFilter);
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
            this.logger.warn("The provided category [{}] is not supported by the Care Pathways vocabulary.", category);
            return Collections.emptyList();
        }
        final boolean isId = isId(input);
        // If a custom filter is provided, use that.
        final String filter = StringUtils.defaultIfBlank(customFilter, generateDefaultFilter(category, isId));
        return search(input, maxResults, sort, filter, isId);
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
            for (final String sortItem :sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, " "),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-") ? ORDER.desc : ORDER.asc);
            }
        }
        return query;
    }

    /**
     * Generates the default filter based on the provided {@code category}, if the query is not an ID.
     *
     * @param category the term category to search
     * @param isId true iff the query is an identifier, false otherwise
     * @return the appropriate filter, or null if the query is an ID
     */
    @Nullable
    private String generateDefaultFilter(@Nonnull final String category, final boolean isId)
    {
        return isId
            ? null
            : TEST_CATEGORY.equals(category) ? DEFAULT_TEST_FILTER : DEFAULT_CARE_FILTER;
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
     * Get the value associated with the current term being processed from the provided {@code row}.
     *
     * @param row the {@link CSVRecord} being processed
     * @return a string representation of the current term being processed
     */
    @Nonnull
    private String getNode(@Nonnull final CSVRecord row)
    {
        return row.get(row.size() - 1);
    }

    /**
     * Extract term name from the term value string.
     *
     * @param value the string containing the name and identifier information for the term
     * @return the name for the term
     * @throws IllegalArgumentException if no name is specified
     */
    @Nonnull
    private String getNodeName(@Nonnull final String value)
    {
        final String name = StringUtils.substringBefore(value, OPEN);
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Term " + value + " is missing a name");
        }
        return name.trim();
    }

    /**
     * Extract term ID from term value string.
     *
     * @param value the string containing the name and identifier information for the term
     * @return the identifier for the term
     * @throws IllegalArgumentException if no id is specified
     */
    @Nonnull
    private String getNodeId(@Nonnull final String value)
    {
        final String id = StringUtils.substringBetween(value, OPEN, CLOSE);
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Term " + value + " is missing an id");
        }
        return id.trim();
    }

    /**
     * Retrieve all relevant data from {@code row}.
     *
     * @param row the {@link CSVRecord} being processed
     * @return the {@link CPNode} object containing all relevant data from {@code row}
     */
    @Nullable
    private CPNode getPathData(@Nonnull final CSVRecord row)
    {
        final int rowSize = row.size();
        // Blank line, skip.
        if (rowSize < 1) {
            return null;
        }
        final String value = getNode(row);
        // May throw an IllegalArgumentException here, if data is bad.
        final String id = getNodeId(value);
        final String name = getNodeName(value);
        // Create a new CPNode object for a node with some ID and name.
        final CPNode node = new CPNode(id, name);
        // Not root node.
        return (rowSize > 1) ? setParentAndAncestors(node, row, rowSize - 2) : node;
    }

    /**
     * Sets parents and ancestors for {@code node}, as per the data in {@code row}.
     *
     * @param node the {@link CPNode} where ancestor data will be written
     * @param row the {@link CSVRecord} from which data will be retrieved
     * @param lastIdx the last index (inclusive) where an ancestor can be found
     */
    @Nonnull
    private CPNode setParentAndAncestors(@Nonnull final CPNode node, @Nonnull final CSVRecord row, final int lastIdx)
    {
        // May throw an IllegalArgumentException if data is bad.
        final String parent = getNodeId(row.get(lastIdx));
        node.addParent(parent);
        node.addAncestor(parent);
        // We already have all the data.
        if (lastIdx == 0) {
            return node;
        }
        // There are more ancestors.
        for (int i = 0; i < lastIdx; i++) {
            // May throw an IllegalArgumentException if data is bad.
            node.addAncestor(getNodeId(row.get(i)));
        }
        return node;
    }

    /**
     * Merges the data from {@code n1} and {@code n2}, where the two nodes are assumed to contain data for the same
     * vocabulary term. No check is performed to ensure that the two nodes refer to the same term.
     *
     * @param n1 a {@link CPNode} containing data for term with ID: {@link CPNode#getId()}, which is equal to {@code n1}
     * @param n2 a {@link CPNode} containing data for term with ID: {@link CPNode#getId()}, which is equal to {@code n2}
     * @return a {@link CPNode} with merged data from {@code n1} and {@code n2}
     */
    @Nonnull
    private CPNode mergeNodes(@Nonnull final CPNode n1, @Nonnull final CPNode n2)
    {
        n1.addParents(n2.getParents());
        n1.addAncestors(n2.getAncestors());
        return n1;
    }

    /**
     * Builds a {@link SolrInputDocument} from the data stored in {@code node}.
     *
     * @param node the {@link CPNode} containing date for a care pathways vocabulary term
     * @return a {@link SolrInputDocument} with the data from {@code node}
     */
    private SolrInputDocument buildSolrDoc(final CPNode node)
    {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.addField(ID, node.getId());
        doc.addField(NAME, node.getName());
        doc.addField(IS_A, node.getParents());
        doc.addField(TERM_CATEGORY, node.getAncestors());
        return doc;
    }
}
