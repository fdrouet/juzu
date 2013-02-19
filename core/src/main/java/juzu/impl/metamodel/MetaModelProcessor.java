/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.metamodel;

import juzu.impl.common.Name;
import juzu.impl.compiler.BaseProcessor;
import juzu.impl.compiler.MessageCode;
import juzu.impl.compiler.ProcessingContext;
import juzu.impl.common.Logger;
import juzu.impl.common.Tools;

import javax.annotation.Generated;
import javax.annotation.processing.Completion;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class MetaModelProcessor extends BaseProcessor {

  /** . */
  public static final MessageCode ANNOTATION_UNSUPPORTED = new MessageCode("ANNOTATION_UNSUPPORTED", "The annotation of this element cannot be supported");

  /** . */
  private MetaModelState<?, ?> state;

  /** . */
  private int index;

  /** . */
  private final Logger log = BaseProcessor.getLogger(getClass());

  /** . */
  private HashSet<String> supportedAnnotations;

  @Override
  protected void doInit(ProcessingContext context) {
    log.log("Using processing env " + context.getClass().getName());

    // Try to get state or create new one
    if (state == null) {
      InputStream in = null;
      try {
        FileObject file = getContext().getResource(StandardLocation.SOURCE_OUTPUT, "juzu", "metamodel.ser");
        in = file.openInputStream();
        ObjectInputStream ois = new ObjectInputStream(in);
        state = (MetaModelState<?, ?>)ois.readObject();
        log.log("Loaded model from " + file.toUri());
      }
      catch (Exception e) {
        log.log("Created new meta model");
        MetaModelState<?, ?> metaModel = new MetaModelState(getPluginType(), createMetaModel());
        metaModel.init(getContext());
        state = metaModel;
      }
      finally {
        Tools.safeClose(in);
      }
    }

    //
    HashSet<String> supportedAnnotations = new HashSet<String>();
    for (Class<?> supportedAnnotation : state.context.getSupportedAnnotations()) {
      supportedAnnotations.add(supportedAnnotation.getName());
    }

    //
    this.supportedAnnotations = supportedAnnotations;
    this.index = 0;
  }

  protected abstract Class<? extends MetaModelPlugin<?, ?>> getPluginType();

  protected abstract MetaModel<?, ?> createMetaModel();

  @Override
  protected void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!roundEnv.errorRaised()) {
      if (roundEnv.processingOver()) {
        log.log("APT processing over");

        //
        log.log("Passivating model");
        state.metaModel.prePassivate();

        // Passivate model
        ObjectOutputStream out = null;
        try {
          FileObject file = getContext().createResource(StandardLocation.SOURCE_OUTPUT, "juzu", "metamodel.ser");
          out = new ObjectOutputStream(file.openOutputStream());
          out.writeObject(state);
          state = null;
        }
        catch (Exception e) {
          e.printStackTrace();
          log.log("Could not passivate model ", e);
        }
        finally {
          Tools.safeClose(out);
        }
      }
      else {
        log.log("Starting APT round #" + index);

        //
        if (index == 0) {
          log.log("Activating model");
          state.metaModel.postActivate(getContext());
        }

        //
        LinkedHashMap<AnnotationKey, AnnotationState> updates = new LinkedHashMap<AnnotationKey, AnnotationState>();
        for (TypeElement annotationElt : annotations) {
          if (supportedAnnotations.contains(annotationElt.getQualifiedName().toString())) {
            log.log("Processing elements for annotation for " + annotationElt.getQualifiedName());
            for (Element annotatedElt : roundEnv.getElementsAnnotatedWith(annotationElt)) {
              if (annotatedElt.getAnnotation(Generated.class) == null) {
                log.log("Processing element " + annotatedElt);
                for (AnnotationMirror annotationMirror : annotatedElt.getAnnotationMirrors()) {
                  if (annotationMirror.getAnnotationType().asElement().equals(annotationElt)) {
                    AnnotationKey key = new AnnotationKey(annotatedElt, annotationMirror);
                    AnnotationState state = AnnotationState.create(annotationMirror);
                    updates.put(key, state);
                  }
                }
              }
            }
          }
        }

        //
        log.log("Process annotations");
        state.context.processAnnotations(updates.entrySet());

        //
        log.log("Post processing model");
        state.metaModel.postProcessAnnotations();

        //
        log.log("Process events");
        state.metaModel.processEvents();

        //
        log.log("Post process events");
        state.metaModel.postProcessEvents();

        //
        log.log("Ending APT round #" + index++);
      }
    }
  }

  @Override
  public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
    Iterable<? extends Completion> completions;
    // For now we don't provide completion when element is absent
    if (element != null) {
      // Get state (but we won't save it)
      log.log("Activating model");
      state.metaModel.postActivate(getContext());
      AnnotationKey annotationKey = new AnnotationKey(element, Name.parse(((TypeElement)annotation.getAnnotationType().asElement()).getQualifiedName().toString()));
      AnnotationState annotationState = AnnotationState.create(annotation);
      completions = state.context.getCompletions(annotationKey, annotationState, member.getSimpleName().toString(), userText);
    } else {
      completions = Collections.emptyList();
    }
    return completions != null ? completions : Collections.<Completion>emptyList();
  }
}
