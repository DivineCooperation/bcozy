package org.openbase.bco.bcozy.view.generic;

import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;

public interface EmphasisDescriptionProvider {
    String getLabel(Category primaryCategory);
}
