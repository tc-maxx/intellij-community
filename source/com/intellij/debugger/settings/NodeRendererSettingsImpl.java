package com.intellij.debugger.settings;

import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.impl.watch.render.ArrayRenderer;
import com.intellij.debugger.ui.impl.watch.render.ClassRenderer;
import com.intellij.debugger.ui.impl.watch.render.PrimitiveRenderer;
import com.intellij.debugger.ui.impl.watch.render.DefaultRendererProvider;
import com.intellij.debugger.ui.tree.render.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.NamedJDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.InternalIterator;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: lex
 * Date: Sep 18, 2003
 * Time: 8:00:25 PM
 */
public class NodeRendererSettingsImpl extends NodeRendererSettings implements Cloneable, NamedJDOMExternalizable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.settings.NodeRendererSettingsImpl");
  private static final String AUTO_NODE = "node";

  private static final int VERSION = 3;

  private EventDispatcher<NodeRendererSettingsListener> myDispatcher = EventDispatcher.create(NodeRendererSettingsListener.class);
  private List<AutoRendererNode> myRepresentationNodes = new ArrayList<AutoRendererNode>();
  private final DefaultRendererProvider myDefaultRendererProvider;

  public NodeRendererSettingsImpl(DefaultRendererProvider defaultRendererProvider) {
    myDefaultRendererProvider = defaultRendererProvider;
  }

  public String getComponentName() {
    return "NodeRendererSettings";
  }

  public void initComponent() { }

  public void disposeComponent() {
  }

  public String getExternalFileName() {
    return "debugger.renderers";
  }

  public NodeRendererSettingsImpl clone() {
    NodeRendererSettingsImpl result = null;
    try {
      result = (NodeRendererSettingsImpl)super.clone();
    }
    catch (CloneNotSupportedException e) {
      LOG.error(e);
    }
    result.myDispatcher = EventDispatcher.create(NodeRendererSettingsListener.class);
    result.myRepresentationNodes = new ArrayList<AutoRendererNode>();
    for (Iterator<AutoRendererNode> iterator = myRepresentationNodes.iterator(); iterator.hasNext();) {
      AutoRendererNode autoRendererNode = iterator.next();
      result.addNode(autoRendererNode.clone());
    }

    return result;
  }

  public boolean equals(Object o) {
    if(!(o instanceof NodeRendererSettingsImpl)) return false;

    return DebuggerUtilsEx.externalizableEqual(this, (NodeRendererSettingsImpl)o);
  }

  public void addListener(NodeRendererSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  public void removeListener(NodeRendererSettingsListener listener) {
    myDispatcher.removeListener(listener);
  }

  public void writeExternal(final Element element) throws WriteExternalException {
    for (Iterator<AutoRendererNode> iterator = myRepresentationNodes.iterator(); iterator.hasNext();) {
      AutoRendererNode autoRendererNode = iterator.next();
      Element nodeElement = new Element(AUTO_NODE);
      autoRendererNode.writeExternal(nodeElement);
      element.addContent(nodeElement);
    }
    element.setAttribute("VERSION", String.valueOf(VERSION));
  }

  public void readExternal(final Element root) {
    String versionAttrib = root.getAttributeValue("VERSION");
    int configurationVersion = -1;
    if (versionAttrib != null) {
      try {
        configurationVersion = Integer.parseInt(versionAttrib);
      }
      catch (NumberFormatException e) {
        configurationVersion = -1;
      }
    }
    if(configurationVersion != VERSION) {
      return;
    }

    List<Element> children = root.getChildren(AUTO_NODE);

    myRepresentationNodes.clear();
    for (Iterator<Element> iterator = children.iterator(); iterator.hasNext();) {
      Element nodeRepresentation = iterator.next();
      try {
        addNode(AutoRendererNode.read(nodeRepresentation));
      } catch (Exception e) {
        LOG.debug(e);
      }
    }

    myDispatcher.getMulticaster().renderersChanged();
  }

  private void addNode(AutoRendererNode nodeRepresentation) {
    LOG.assertTrue(nodeRepresentation != null);
    myRepresentationNodes.add(nodeRepresentation);
  }

  public void addRenderer(NodeRenderer renderer) {
    addNode(new AutoRendererNode(renderer));
  }

  public void removeRenderer(NodeRenderer renderer) {
    myRepresentationNodes.remove(renderer);
  }

  public List<NodeRenderer> getAllRenderers() {
    List<NodeRenderer> result = new ArrayList<NodeRenderer>();

    for (Iterator<AutoRendererNode> iterator = myRepresentationNodes.iterator(); iterator.hasNext();) {
      AutoRendererNode autoRendererNode = iterator.next();
      result.add(autoRendererNode.getRenderer());
    }
    result.add(myDefaultRendererProvider.getArrayRenderer());
    result.add(myDefaultRendererProvider.getClassRenderer());
    result.add(myDefaultRendererProvider.getPrimitiveRenderer());

    return result;
  }

  public boolean isDefault(Renderer renderer) {
    return renderer == myDefaultRendererProvider.getArrayRenderer() ||
           renderer == myDefaultRendererProvider.getClassRenderer() ||
           renderer == myDefaultRendererProvider.getPrimitiveRenderer();
  }

  public List<AutoRendererNode> getAutoNodes() {
    List<AutoRendererNode> result = new ArrayList<AutoRendererNode>();

    for (Iterator<AutoRendererNode> iterator = myRepresentationNodes.iterator(); iterator.hasNext();) {
      AutoRendererNode autoRendererNode = iterator.next();
      result.add(autoRendererNode);
    }

    return result;
  }

  public void setAutoNodes(List<AutoRendererNode> nodes) {
    myRepresentationNodes = nodes;
    fireRenderersChanged();
  }  

  public List<NodeRenderer> getRenderersByProvider(RendererProvider rendererProvider) {
    List<NodeRenderer> result = new ArrayList<NodeRenderer>();

    for (Iterator<AutoRendererNode> iterator = myRepresentationNodes.iterator(); iterator.hasNext();) {
      AutoRendererNode autoRendererNode = iterator.next();
      if(autoRendererNode.getRenderer().getRendererProvider() == rendererProvider) result.add(autoRendererNode.getRenderer());
    }
    return result;
  }

  public void iterateRenderers(InternalIterator<AutoRendererNode> iterator) {
    for (Iterator<AutoRendererNode> it = myRepresentationNodes.iterator(); it.hasNext();) {
      AutoRendererNode autoRendererNode = it.next();
      iterator.visit(autoRendererNode);
    }
  }

  public PrimitiveRenderer getPrimitiveRenderer() {
    return myDefaultRendererProvider.getPrimitiveRenderer();
  }

  public ArrayRenderer getArrayRenderer() {
    return myDefaultRendererProvider.getArrayRenderer();
  }

  public ClassRenderer getClassRenderer() {
    return myDefaultRendererProvider.getClassRenderer();
  }

  public void fireRenderersChanged() {
    myDispatcher.getMulticaster().renderersChanged();
  }

  public static NodeRendererSettingsImpl getInstanceEx() {
    return (NodeRendererSettingsImpl)getInstance();
  }
}
