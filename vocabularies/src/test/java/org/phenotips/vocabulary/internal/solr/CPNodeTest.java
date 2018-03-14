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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link CPNodeTest} class.
 */
public class CPNodeTest
{
    /** An identifier for the node. */
    private static final String NODE_ID = "CP:1";

    /** A name for the node. */
    private static final String NODE_NAME = "nodeName";

    private static final String PARENT_ID_1 = "abc";

    private static final String PARENT_ID_2 = "def";

    private static final String PARENT_ID_3 = "ghi";

    private CPNode node;

    @Before
    public void setUp()
    {
        this.node = new CPNode(NODE_ID, NODE_NAME);
    }

    @Test
    public void getIdReturnsSpecifiedId()
    {
        Assert.assertEquals(NODE_ID, this.node.getId());
    }

    @Test
    public void getNameReturnsSpecifiedName()
    {
        Assert.assertEquals(NODE_NAME, this.node.getName());
    }

    @Test
    public void getParentsReturnsEmptyCollectionWhenNoParentsSet()
    {
        final Collection<String> parents = this.node.getParents();
        Assert.assertNotNull(parents);
        Assert.assertTrue(parents.isEmpty());
    }

    @Test
    public void getParentsReturnsEmptyCollectionWhenEmptyParentCollectionSet()
    {
        this.node.setParents(new HashSet<>());
        final Collection<String> parents = this.node.getParents();
        Assert.assertNotNull(parents);
        Assert.assertTrue(parents.isEmpty());
    }

    @Test
    public void getParentsReturnsExpectedCollection()
    {
        final Set<String> newParents = new HashSet<>();
        newParents.add(PARENT_ID_1);
        this.node.setParents(newParents);

        Assert.assertEquals(newParents, this.node.getParents());
    }

    @Test
    public void getAncestorsReturnsEmptyCollectionWhenNoParentsSet()
    {
        final Collection<String> ancestors = this.node.getAncestors();
        Assert.assertNotNull(ancestors);
        Assert.assertTrue(ancestors.isEmpty());
    }

    @Test
    public void getAncestorsReturnsEmptyCollectionWhenEmptyParentCollectionSet()
    {
        this.node.setAncestors(new HashSet<>());
        final Collection<String> ancestors = this.node.getAncestors();
        Assert.assertNotNull(ancestors);
        Assert.assertTrue(ancestors.isEmpty());
    }

    @Test
    public void getAncestorsReturnsExpectedCollection()
    {
        final Set<String> newAncestors = new HashSet<>();
        newAncestors.add(PARENT_ID_1);
        this.node.setAncestors(newAncestors);

        Assert.assertEquals(newAncestors, this.node.getAncestors());
    }

    @Test
    public void addParentWorksWhenNoParentsWereSetYet()
    {
        this.node.addParent(PARENT_ID_1);
        Assert.assertEquals(1, this.node.getParents().size());
        Assert.assertTrue(this.node.getParents().contains(PARENT_ID_1));
    }

    @Test
    public void addParentNoNullAdded()
    {
        this.node.addParent(null);
        Assert.assertTrue(this.node.getParents().isEmpty());
    }

    @Test
    public void addParentNoEmptyStrAdded()
    {
        this.node.addParent(StringUtils.EMPTY);
        Assert.assertTrue(this.node.getParents().isEmpty());
    }

    @Test
    public void addParentNoBlankStrAdded()
    {
        this.node.addParent(StringUtils.SPACE);
        Assert.assertTrue(this.node.getParents().isEmpty());
    }

    @Test
    public void addParentAddsOneAfterAnother()
    {
        this.node.addParent(PARENT_ID_1);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(1, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));

        this.node.addParent(PARENT_ID_2);
        retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addParentNoDuplicateAdded()
    {
        this.node.addParent(PARENT_ID_1);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(1, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));

        this.node.addParent(PARENT_ID_2);
        retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        this.node.addParent(PARENT_ID_1);
        retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addParentWorksWhenUnmodifiableCollectionSet()
    {
        final Set<String> newParents = new HashSet<>();
        newParents.add(PARENT_ID_1);
        this.node.setParents(Collections.unmodifiableSet(newParents));
        Assert.assertEquals(newParents, this.node.getParents());

        final Set<String> expected = new HashSet<>();
        expected.add(PARENT_ID_1);
        expected.add(PARENT_ID_2);
        this.node.addParent(PARENT_ID_2);
        Assert.assertEquals(expected, this.node.getParents());
    }

    @Test
    public void addAncestorsWorksWhenNoAncestorsWereSetYet()
    {
        this.node.addAncestor(PARENT_ID_1);
        Assert.assertEquals(1, this.node.getAncestors().size());
        Assert.assertTrue(this.node.getAncestors().contains(PARENT_ID_1));
    }

    @Test
    public void addAncestorNoNullAdded()
    {
        this.node.addAncestor(null);
        Assert.assertTrue(this.node.getAncestors().isEmpty());
    }

    @Test
    public void addAncestorNoEmptyStrAdded()
    {
        this.node.addAncestor(StringUtils.EMPTY);
        Assert.assertTrue(this.node.getAncestors().isEmpty());
    }

    @Test
    public void addAncestorNoBlankStrAdded()
    {
        this.node.addAncestor(StringUtils.SPACE);
        Assert.assertTrue(this.node.getAncestors().isEmpty());
    }

    @Test
    public void addAncestorAddsOneAfterAnother()
    {
        this.node.addAncestor(PARENT_ID_1);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(1, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));

        this.node.addAncestor(PARENT_ID_2);
        retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addAncestorNoDuplicateAdded()
    {
        this.node.addAncestor(PARENT_ID_1);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(1, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));

        this.node.addAncestor(PARENT_ID_2);
        retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        this.node.addAncestor(PARENT_ID_1);
        retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addAncestorWorksWhenUnmodifiableCollectionIsSet()
    {
        final Set<String> newAncestors = new HashSet<>();
        newAncestors.add(PARENT_ID_1);
        this.node.setAncestors(Collections.unmodifiableSet(newAncestors));
        Assert.assertEquals(newAncestors, this.node.getAncestors());

        final Set<String> expected = new HashSet<>();
        expected.add(PARENT_ID_1);
        expected.add(PARENT_ID_2);
        this.node.addAncestor(PARENT_ID_2);
        Assert.assertEquals(expected, this.node.getAncestors());
    }

    @Test
    public void addParentsWorksAsExpectedForACollection()
    {
        final List<String> parents = Arrays.asList(PARENT_ID_1, PARENT_ID_2);
        this.node.addParents(parents);
        final Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addParentsAddsMoreParents()
    {
        final List<String> parents = Arrays.asList(PARENT_ID_1, PARENT_ID_2);
        this.node.addParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        final List<String> moreParents = Arrays.asList(PARENT_ID_2, PARENT_ID_3);
        this.node.addParents(moreParents);
        retrieved = this.node.getParents();
        Assert.assertEquals(3, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
        Assert.assertTrue(retrieved.contains(PARENT_ID_3));
    }

    @Test
    public void addParentsModifyingAddedCollectionDoesNotModifyParents()
    {
        final Collection<String> parents = new HashSet<>();
        parents.add(PARENT_ID_1);
        parents.add(PARENT_ID_2);

        this.node.addParents(parents);
        Collection<String> retrieved = this.node.getParents();

        Assert.assertEquals(parents, retrieved);

        parents.add(PARENT_ID_3);
        Assert.assertNotEquals(parents, retrieved);
    }

    @Test
    public void addParentsDoesNotAddNull()
    {
        final List<String> parents = Arrays.asList(PARENT_ID_1, null, PARENT_ID_2);
        this.node.addParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addParentsDoesNotAddEmpty()
    {
        final List<String> parents = Arrays.asList(PARENT_ID_1, StringUtils.EMPTY, PARENT_ID_2);
        this.node.addParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addParentsDoesNotAddBlank()
    {
        final List<String> parents = Arrays.asList(PARENT_ID_1, StringUtils.SPACE, PARENT_ID_2);
        this.node.addParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setParentsSetsCollectionAsExpected()
    {
        final Set<String> parents = new HashSet<>();
        parents.add(PARENT_ID_1);
        parents.add(PARENT_ID_2);
        this.node.setParents(parents);
        final Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setParentsReplacesPreviousCollection()
    {
        Set<String> parents = new HashSet<>();
        parents.add(PARENT_ID_1);
        parents.add(PARENT_ID_2);
        this.node.setParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        parents = new HashSet<>();
        parents.add(PARENT_ID_3);
        this.node.setParents(parents);
        retrieved = this.node.getParents();
        Assert.assertEquals(1, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_3));
    }

    @Test
    public void setParentsModifyingSetCollectionDoesNotModifyParents()
    {
        Set<String> parents = new HashSet<>();
        parents.add(PARENT_ID_1);
        parents.add(PARENT_ID_2);
        this.node.setParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        parents.add(PARENT_ID_3);
        retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setParentsFiltersNulls()
    {
        Set<String> parents = new HashSet<>();
        parents.add(PARENT_ID_1);
        parents.add(null);
        parents.add(PARENT_ID_2);
        this.node.setParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setParentsFiltersEmptyStrings()
    {
        Set<String> parents = new HashSet<>();
        parents.add(PARENT_ID_1);
        parents.add(StringUtils.EMPTY);
        parents.add(PARENT_ID_2);
        this.node.setParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setParentsFiltersBlankStrings()
    {
        Set<String> parents = new HashSet<>();
        parents.add(PARENT_ID_1);
        parents.add(StringUtils.SPACE);
        parents.add(PARENT_ID_2);
        this.node.setParents(parents);
        Collection<String> retrieved = this.node.getParents();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addAncestorsWorksAsExpectedForACollection()
    {
        final List<String> ancestors = Arrays.asList(PARENT_ID_1, PARENT_ID_2);
        this.node.addAncestors(ancestors);
        final Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addAncestorsAddsMoreAncestors()
    {
        final List<String> ancestors = Arrays.asList(PARENT_ID_1, PARENT_ID_2);
        this.node.addAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        final List<String> moreAncestors = Arrays.asList(PARENT_ID_2, PARENT_ID_3);
        this.node.addAncestors(moreAncestors);
        retrieved = this.node.getAncestors();
        Assert.assertEquals(3, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
        Assert.assertTrue(retrieved.contains(PARENT_ID_3));
    }

    @Test
    public void addAncestorsModifyingAddedCollectionDoesNotModifyAncestors()
    {
        final Collection<String> ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_1);
        ancestors.add(PARENT_ID_2);

        this.node.addAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();

        Assert.assertEquals(ancestors, retrieved);

        ancestors.add(PARENT_ID_3);
        Assert.assertNotEquals(ancestors, retrieved);
    }

    @Test
    public void addAncestorsDoesNotAddNull()
    {
        final List<String> ancestors = Arrays.asList(PARENT_ID_1, null, PARENT_ID_2);
        this.node.addAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addAncestorsDoesNotAddEmpty()
    {
        final List<String> ancestors = Arrays.asList(PARENT_ID_1, StringUtils.EMPTY, PARENT_ID_2);
        this.node.addAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void addAncestorsDoesNotAddBlank()
    {
        final List<String> ancestors = Arrays.asList(PARENT_ID_1, StringUtils.SPACE, PARENT_ID_2);
        this.node.addAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setAncestorsSetsCollectionAsExpected()
    {
        final Set<String> ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_1);
        ancestors.add(PARENT_ID_2);
        this.node.setAncestors(ancestors);
        final Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setAncestorsReplacesPreviousCollection()
    {
        Set<String> ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_1);
        ancestors.add(PARENT_ID_2);
        this.node.setAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_3);
        this.node.setAncestors(ancestors);
        retrieved = this.node.getAncestors();
        Assert.assertEquals(1, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_3));
    }

    @Test
    public void setAncestorsModifyingSetCollectionDoesNotModifyAncestors()
    {
        Set<String> ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_1);
        ancestors.add(PARENT_ID_2);
        this.node.setAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));

        ancestors.add(PARENT_ID_3);
        retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setAncestorsFiltersNulls()
    {
        Set<String> ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_1);
        ancestors.add(null);
        ancestors.add(PARENT_ID_2);
        this.node.setAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setAncestorsFiltersEmptyStrings()
    {
        Set<String> ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_1);
        ancestors.add(StringUtils.EMPTY);
        ancestors.add(PARENT_ID_2);
        this.node.setAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }

    @Test
    public void setAncestorsFiltersBlankStrings()
    {
        Set<String> ancestors = new HashSet<>();
        ancestors.add(PARENT_ID_1);
        ancestors.add(StringUtils.SPACE);
        ancestors.add(PARENT_ID_2);
        this.node.setAncestors(ancestors);
        Collection<String> retrieved = this.node.getAncestors();
        Assert.assertEquals(2, retrieved.size());
        Assert.assertTrue(retrieved.contains(PARENT_ID_1));
        Assert.assertTrue(retrieved.contains(PARENT_ID_2));
    }
}
