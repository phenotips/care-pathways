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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;

/**
 * A class that holds data pertaining to a care pathways test or care node. Has convenience methods such as
 * {@link #addAncestor(String)}, {@link #addAncestors(Collection)}, {@link #setAncestors(Set)}, {@link #getAncestors()},
 * etc. for easy data access.
 */
class CPNode
{
    /** The node name. */
    private final String name;

    /** The node id. */
    private final String id;

    /** The set of parent identifiers for the node. */
    private Set<String> isA;

    /** The set of ancestor identifiers for the node. */
    private Set<String> termCategory;

    /**
     * A simple constructor, requiring the node {@code id} and {@code name}.
     *
     * @param id the node identifier, as string; must not be blank
     * @param name the node name, as string; must not be blank
     */
    CPNode(@Nonnull final String id, @Nonnull final String name)
    {
        this.id = id;
        this.name = name;
        this.isA = new HashSet<>();
        this.termCategory = new HashSet<>();
    }

    /**
     * Gets the node {@link #id}.
     *
     * @return the node identifier
     */
    @Nonnull
    String getId()
    {
        return this.id;
    }

    /**
     * Gets the node {@link #name}.
     *
     * @return the node name
     */
    @Nonnull
    String getName()
    {
        return this.name;
    }

    /**
     * Returns the collection of parent identifiers for the node.
     *
     * @return a collection of parents
     */
    @Nonnull
    Collection<String> getParents()
    {
        return Collections.unmodifiableSet(this.isA);
    }

    /**
     * Returns the collection of ancestor identifiers for the node.
     *
     * @return a collection of identifiers
     */
    @Nonnull
    Collection<String> getAncestors()
    {
        return Collections.unmodifiableSet(this.termCategory);
    }

    /**
     * Adds a parent to the collection of {@link #isA parents}, and returns the updated collection.
     *
     * @param parent the parent identifier to add
     * @return the updated collection of parent identifiers
     */
    @Nonnull
    Collection<String> addParent(@Nullable final String parent)
    {
        CollectionUtils.addIgnoreNull(this.isA, parent);
        return Collections.unmodifiableSet(this.isA);
    }

    /**
     * Adds an ancestor to the collection of {@link #termCategory ancestors}, and returns the updated collection.
     *
     * @param ancestor the ancestor identifier to add
     * @return the updated collection of ancestor identifiers
     */
    @Nonnull
    Collection<String> addAncestor(@Nullable final String ancestor)
    {
        CollectionUtils.addIgnoreNull(this.termCategory, ancestor);
        return Collections.unmodifiableSet(this.termCategory);
    }

    /**
     * Adds a parents to the collection of {@link #isA parents}, and returns the updated collection.
     *
     * @param parents the collection of parent identifiers to add
     * @return the updated collection of parent identifiers
     */
    @Nonnull
    Collection<String> addParents(@Nullable final Collection<String> parents)
    {
        if (CollectionUtils.isNotEmpty(parents)) {
            this.isA.addAll(parents);
        }
        return Collections.unmodifiableSet(this.isA);
    }

    /**
     * Replaces the collection of {@link #isA parents} with {@code parents}, and returns the updated collection.
     *
     * @param parents the new collection of parent identifiers
     * @return the updated collection of parent identifiers
     */
    @Nonnull
    Collection<String> setParents(@Nullable final Set<String> parents)
    {
        if (parents != null) {
            this.isA = parents;
        }
        return Collections.unmodifiableSet(this.isA);
    }

    /**
     * Adds ancestors to the collection of {@link #termCategory ancestors}, and returns the updated collection.
     *
     * @param ancestors the collection of ancestor identifiers to add
     * @return the updated collection of ancestor identifiers
     */
    @Nonnull
    Collection<String> addAncestors(@Nullable final Collection<String> ancestors)
    {
        if (CollectionUtils.isNotEmpty(ancestors)) {
            this.termCategory.addAll(ancestors);
        }
        return Collections.unmodifiableSet(this.termCategory);
    }

    /**
     * Replaces the collection of {@link #termCategory ancestors} with {@code ancestors}, and returns the updated
     * collection.
     *
     * @param ancestors the new collection of ancestor identifiers
     * @return the updated collection of ancestor identifiers
     */
    @Nonnull
    Collection<String> setAncestors(@Nullable final Set<String> ancestors)
    {
        if (ancestors != null) {
            this.termCategory = ancestors;
        }
        return Collections.unmodifiableSet(this.termCategory);
    }
}
