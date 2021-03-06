package de.westnordost.streetcomplete.quests.crossing_type;

import android.os.Bundle;
import java.util.List;

import javax.inject.Inject;

import de.westnordost.streetcomplete.data.osm.SimpleOverpassQuestType;
import de.westnordost.streetcomplete.data.osm.changes.StringMapChangesBuilder;
import de.westnordost.streetcomplete.data.osm.download.OverpassMapDataDao;
import de.westnordost.streetcomplete.quests.AbstractQuestAnswerFragment;

public class AddCrossingType extends SimpleOverpassQuestType
{
    @Inject public AddCrossingType(OverpassMapDataDao overpassServer)
    {
        super(overpassServer);
    }

    @Override
    protected String getTagFilters()
    {
        return "nodes with highway=crossing and !crossing";
    }

    public AbstractQuestAnswerFragment createForm()
    {
        return new AddCrossingTypeForm();
    }

    public void applyAnswerTo(Bundle answer, StringMapChangesBuilder changes)
    {
        List<String> values = answer.getStringArrayList(AddCrossingTypeForm.OSM_VALUES);
        if(values != null  && values.size() == 1)
        {
            changes.add("crossing", values.get(0));
        }
    }

    @Override public String getCommitMessage()
    {
        return "Add crossing type";
    }

    @Override public String getIconName() {
        return "pedestrian_crossing";
    }
}
