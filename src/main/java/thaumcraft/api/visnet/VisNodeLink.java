package thaumcraft.api.visnet;

import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Связи одного звена сети вис: родитель, дети и счётчик переподключения — та часть TileVisNode оригинала,
 * которая одинакова у источников и реле.
 */
public final class VisNodeLink {
    @Nullable
    private WeakReference<VisNode> parent;
    private List<WeakReference<VisNode>> children = new ArrayList<>();
    private int counter;
    private boolean registered;
    public boolean refresh;

    @Nullable
    public VisNode getParent() {
        VisNode node = parent == null ? null : parent.get();
        return node != null && !node.isVisNodeRemoved() ? node : null;
    }

    public void setParent(@Nullable VisNode node) {
        parent = node == null ? null : new WeakReference<>(node);
    }

    public List<WeakReference<VisNode>> getChildren() {
        return children;
    }

    public int getCounter() {
        return counter;
    }

    /** Реле передаёт запрос родителю. */
    public int consumeFromParent(VisNode self, Aspect aspect, int amount) {
        VisNode up = getParent();
        if (up == null) {
            return 0;
        }
        int out = up.consumeVis(aspect, amount);
        if (out > 0) {
            self.triggerConsumeEffect(aspect);
        }
        return out;
    }

    /** Раз в две секунды звено проверяет, видны ли ещё дети, и ищет себе родителя, если остался без него. */
    public void serverTick(VisNode self) {
        if (counter++ % 40 != 0 && !refresh) {
            return;
        }
        if (!refresh) {
            for (WeakReference<VisNode> ref : children) {
                VisNode child = ref.get();
                if (child == null || child.isVisNodeRemoved() || !VisNetHandler.canNodeBeSeen(self, child)) {
                    refresh = true;
                    break;
                }
            }
        }
        if (refresh) {
            for (WeakReference<VisNode> ref : children) {
                VisNode child = ref.get();
                if (child != null) {
                    child.visLink().refresh = true;
                }
            }
            children.clear();
            parent = null;
        }
        if (self.isVisSource()) {
            if (!registered) {
                VisNetHandler.addSource(self);
                registered = true;
            }
        } else if (getParent() == null) {
            setParent(VisNetHandler.addNode(self));
            refresh = true;
        }
        if (refresh) {
            self.parentChanged();
        }
        refresh = false;
    }

    /** Звено убрано: дети остаются без родителя и будут искать нового. */
    public void remove(VisNode self) {
        for (WeakReference<VisNode> ref : new ArrayList<>(children)) {
            VisNode child = ref.get();
            if (child != null && child != self) {
                child.visLink().remove(child);
            }
        }
        children = new ArrayList<>();
        VisNode up = getParent();
        if (up != null) {
            up.visLink().refresh = true;
        }
        parent = null;
        registered = false;
        self.parentChanged();
        if (self.isVisSource()) {
            VisNetHandler.removeSource(self);
        }
    }
}
