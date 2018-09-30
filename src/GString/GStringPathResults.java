package GString;

import javafx.util.Pair;

import java.util.LinkedList;

public class GStringPathResults {
    public LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> pathResults;
    public GStringNode originNode;
    public GStringEdge startingEdge;

    public GStringPathResults(LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> pathResults, GStringNode originNode, GStringEdge startingEdge) {
        this.pathResults = pathResults;
        this.originNode = originNode;
        this.startingEdge = startingEdge;
    }
}
