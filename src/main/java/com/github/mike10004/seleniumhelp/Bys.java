package com.github.mike10004.seleniumhelp;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class Bys {

    private Bys() {
    }

    public static By conjoin(By first, By second, By...others) {
        final List<By> conditions = Lists.asList(first, second, others);
        return conjoin(conditions);
    }

    public static By conjoin(final Iterable<? extends By> bys) {
        final List<By> conditions = ImmutableList.copyOf(bys);
        checkArgument(!conditions.isEmpty(), "set of conditions must be nonempty");
        return new By() {
            @Override
            public List<WebElement> findElements(SearchContext context) {
                List<Set<WebElement>> sets = new ArrayList<>(conditions.size());
                for (By condition : conditions) {
                    List<WebElement> elements = condition.findElements(context);
                    sets.add(ImmutableSet.copyOf(elements));
                }
                Iterator<Set<WebElement>> results = sets.iterator();
                Set<WebElement> intersection = null;
                while (results.hasNext()) {
                    Set<WebElement> now = results.next();
                    if (intersection == null) {
                        intersection = now;
                    } else {
                        intersection = Sets.intersection(intersection, now);
                    }
                }
                checkState(intersection != null, "bug");
                return ImmutableList.copyOf(intersection);
            }
        };
    }

    public static class Transforms {
        private Transforms() {}

        private static final Function<WebElement, String> elementToText = new Function<WebElement, String>() {
            @Override
            public String apply(WebElement input) {
                return input.getText();
            }
        };

        public static Function<WebElement, String> elementToText() {
            return elementToText;
        }

        public static Function<WebElement, String> elementToAttributeValue(final String attributeName) {
            return new Function<WebElement, String>() {
                @Nullable
                @Override
                public String apply(WebElement input) {
                    return input.getAttribute(attributeName);
                }
            };
        }

        private static final Function<String, String> fuzzy = new Function<String, String>() {

            private final CharMatcher retained = CharMatcher.javaLetter();

            @Override
            public String apply(String input) {
                checkNotNull(input, "input");
                return retained.retainFrom(input).toLowerCase();
            }
        };

        public static Function<String, String> fuzzy() {
            return fuzzy;
        }
    }

    public static class Predicates {
        private Predicates() {}
        public static Predicate<WebElement> attribute(final String attributeName, final Predicate<? super String> valuePredicate) {
            checkNotNull(attributeName, "attributename");
            checkNotNull(valuePredicate, "valuePredicate");
            return new Predicate<WebElement>() {
                @Override
                public boolean apply(@Nullable WebElement input) {
                    if (input == null) {
                        return false;
                    }
                    String attributeValue = input.getAttribute(attributeName);
                    return attributeValue != null && valuePredicate.apply(attributeValue);
                }
            };
        }

        public static Predicate<String> textEqualsIgnoreCase(final String caseInsensitiveText) {
            checkNotNull(caseInsensitiveText);
            return new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String input) {
                    return input != null && caseInsensitiveText.equalsIgnoreCase(input);
                }
            };
        }

        public static Predicate<String> textEqualsFuzzy(final String fuzzilyRequiredText) {
            return compose(
                    com.google.common.base.Predicates.equalTo(fuzzilyRequiredText), Transforms.fuzzy());
        }

        public static Predicate<String> textWithMaxLevenshteinDistanceFrom(final String reference, final int max) {
            checkNotNull(reference, "reference");
            return new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String input) {
                    return input != null && StringUtils.getLevenshteinDistance(reference, input) <= max;
                }
            };
        }

        public static <A, B> Predicate<A> compose(
                Predicate<B> predicate, Function<A, ? extends B> function) {
            return com.google.common.base.Predicates.compose(predicate, function);
        }

        public static Predicate<String> valueIsUriWithPath(final String requiredPath) {
            return valueIsUriWithPath(com.google.common.base.Predicates.equalTo(requiredPath));
        }

       public static Predicate<String> valueIsUriWithPath(final Predicate<String> pathRequirement) {
            checkNotNull(pathRequirement, "pathRequirement");
            return new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    if (input != null) {
                        URI uri;
                        try {
                            uri = new URI(input);
                        } catch (URISyntaxException e) {
                            LoggerFactory.getLogger(Predicates.class).debug("href attribute value is not a valid URI {}", StringUtils.abbreviate(input, 256));
                            return false;
                        }
                        String path = uri.getPath();
                        return pathRequirement.apply(path);
                    } else {
                        return false;
                    }
                }
            };
        }
    }



    public static By attribute(final By preFilter, final String attributeName, Predicate<? super String> predicate) {
        return predicate(preFilter, Predicates.attribute(attributeName, predicate));
    }

    public static By predicate(final By preFilter, final Predicate<? super WebElement> elementPredicate) {
        return new By() {
            @Override
            public List<WebElement> findElements(SearchContext context) {
                List<WebElement> possibles = preFilter.findElements(context);
                List<WebElement> confirmeds = null;
                for (WebElement element : possibles) {
                    boolean applicable = elementPredicate.apply(element);
                    if (applicable) {
                        if (confirmeds == null) {
                            confirmeds = new ArrayList<>(Math.min(possibles.size(), 10));
                        }
                        confirmeds.add(element);
                    }
                }
                return confirmeds == null ? ImmutableList.of() : confirmeds;
            }
        };
    }

    public static By elementWithTextFuzzy(By preFilter, final String fuzzilyRequiredText) {
        return elementWithText(preFilter, Predicates.textEqualsFuzzy(fuzzilyRequiredText));
    }

    public static By elementWithTextIgnoreCase(By preFilter, final String requiredText) {
        return elementWithText(preFilter, Predicates.textEqualsIgnoreCase(requiredText));
    }

    public static By elementWithText(By preFilter, final String requiredText) {
        return elementWithText(preFilter, com.google.common.base.Predicates.equalTo(requiredText));
    }

    public static By elementWithText(By preFilter, final Predicate<String> textPredicate) {
        return predicate(preFilter, Predicates.compose(textPredicate, Transforms.elementToText()));
    }
}
