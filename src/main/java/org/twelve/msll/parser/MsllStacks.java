package org.twelve.msll.parser;

import java.util.ArrayList;

/**
 * A specialized ArrayList implementation for managing a list of `MsllStack` objects.
 *
 * This class is designed to store and manage multiple instances of `MsllStack` used during the
 * parsing process in the Multi-Stack LL (MSLL) parser. By extending ArrayList, it retains
 * the flexibility and performance of the standard ArrayList while providing more contextual clarity.
 *
 * In MSLL parsing, multiple stacks are needed to handle different parsing paths simultaneously, and
 * this class provides a convenient way to store and iterate over these stacks during parsing operations.
 *
 * @author huizi 2024
 */
public class MsllStacks extends ArrayList<MsllStack> {
}
