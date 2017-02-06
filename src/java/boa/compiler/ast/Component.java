/*
 * Copyright 2014, Hridesh Rajan, Robert Dyer, 
 *                 and Iowa State University of Science and Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.compiler.ast;

import boa.compiler.ast.types.AbstractType;
import boa.compiler.visitors.AbstractVisitor;
import boa.compiler.visitors.AbstractVisitorNoArg;
import boa.compiler.visitors.AbstractVisitorNoReturn;

/**
 * 
 * @author rdyer
 * @author hridesh
 */
public class Component extends AbstractType {
    protected Identifier id;
    protected AbstractType typeNode;

    public boolean hasIdentifier() {
        return id != null;
    }

    public Identifier getIdentifier() {
        return id;
    }

    public void setIdentifier(final Identifier id) {
        id.setParent(this);
        this.id = id;
    }

    public AbstractType getType() {
        return typeNode;
    }

    public void setType(final AbstractType typeNode) {
        typeNode.setParent(this);
        this.typeNode = typeNode;
    }

    public Component() {
    }

    public Component(final AbstractType typeNode) {
        this(null, typeNode);
    }

    public Component(final Identifier id, final AbstractType typeNode) {
        if (id != null)
            id.setParent(this);
        if (typeNode != null)
            typeNode.setParent(this);
        this.id = id;
        this.typeNode = typeNode;
    }

    /** {@inheritDoc} */
    @Override
    public <T, A> T accept(final AbstractVisitor<T, A> v, A arg) {
        return v.visit(this, arg);
    }

    /** {@inheritDoc} */
    @Override
    public <A> void accept(final AbstractVisitorNoReturn<A> v, A arg) {
        v.visit(this, arg);
    }

    /** {@inheritDoc} */
    @Override
    public void accept(final AbstractVisitorNoArg v) {
        v.visit(this);
    }

    public Component clone() {
        final Component c = new Component(id.clone(), typeNode.clone());
        copyFieldsTo(c);
        return c;
    }
}
