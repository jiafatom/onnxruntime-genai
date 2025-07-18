/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package ai.onnxruntime.genai;

import java.util.function.Consumer;

/**
 * The `SimpleGenAI` class provides a simple usage example of the GenAI API. It works with a model
 * that generates text based on a prompt, processing a single prompt at a time.
 *
 * <p>Usage:
 *
 * <ul>
 *   <li>Create an instance of the class with the path to the model. The path should also contain
 *       the GenAI configuration files.
 *   <li>Call createGeneratorParams with the prompt text.
 *   <li>Set any other search options via the GeneratorParams object as needed using
 *       `setSearchOption`.
 *   <li>Call generate with the GeneratorParams object and an optional listener.
 * </ul>
 *
 * <p>The listener is used as a callback mechanism so that tokens can be used as they are generated.
 * It should be an instance of a type that implements the Consumer&lt;String&gt; interface.
 */
public class SimpleGenAI implements AutoCloseable {
  private Model model;
  private Tokenizer tokenizer;

  /**
   * Construct a SimpleGenAI instance from model path.
   *
   * @param modelPath The path to the GenAI model.
   * @throws GenAIException If the call to the GenAI native API fails.
   */
  public SimpleGenAI(String modelPath) throws GenAIException {
    model = new Model(modelPath);
    tokenizer = new Tokenizer(model);
  }

  /**
   * Get the underlying model object.
   *
   * @return The model object.
   * @throws GenAIException on failure
   */
  public Model getModel() throws GenAIException {
    return model;
  }

  /**
   * Create the generator parameters and add the prompt text. The user can set other search options
   * via the GeneratorParams object prior to running `generate`.
   *
   * @return The generator parameters.
   * @throws GenAIException on failure
   */
  public GeneratorParams createGeneratorParams() throws GenAIException {
    return new GeneratorParams(model);
  }

  /**
   * Generate text based on the prompt and settings in GeneratorParams.
   *
   * <p>NOTE: This only handles a single sequence of input (i.e. a single prompt which equates to
   * batch size of 1)
   *
   * @param generatorParams The prompt and settings to run the model with.
   * @param prompt The prompt text to encode.
   * @param listener Optional callback for tokens to be provided as they are generated. NOTE: Token
   *     generation will be blocked until the listener's `accept` method returns. `listener` will be
   *     called within the token generation loop and these calls will be made sequentially, not
   *     concurrently.
   * @return The generated text.
   * @throws GenAIException on failure
   */
  public String generate(GeneratorParams generatorParams, String prompt, Consumer<String> listener)
      throws GenAIException {
    String result;
    try {
      int[] output_ids;

      if (listener != null) {
        try (TokenizerStream stream = tokenizer.createStream();
            Generator generator = new Generator(model, generatorParams)) {
          // iterate (which calls computeLogits, generateNextToken, getLastTokenInSequence and
          // isDone)
          generator.appendTokenSequences(tokenizer.encode(prompt));
          for (int token_id : generator) {
            // decode and call listener
            String token = stream.decode(token_id);
            listener.accept(token);
          }

          output_ids = generator.getSequence(0);
        } catch (GenAIException e) {
          throw new GenAIException("Token generation loop failed.", e);
        }
      } else {
        try (Generator generator = new Generator(model, generatorParams)) {
          generator.appendTokenSequences(tokenizer.encode(prompt));
          for (int token_id : generator) {
            // do nothing
          }
          output_ids = generator.getSequence(0);
        } catch (GenAIException e) {
          throw new GenAIException("Token generation loop failed.", e);
        }
      }

      result = tokenizer.decode(output_ids);
    } catch (GenAIException e) {
      throw new GenAIException("Failed to create Tokenizer.", e);
    }

    return result;
  }

  @Override
  public void close() {
    if (tokenizer != null) {
      tokenizer.close();
      tokenizer = null;
    }
    if (model != null) {
      model.close();
      model = null;
    }
  }
}
