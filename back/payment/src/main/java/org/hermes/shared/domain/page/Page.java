package org.hermes.shared.domain.page;

import org.hermes.shared.api.ResponseHTTP;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Page<T> {

    private final List<T> content;
    private final long totalElements;
    private final int page;
    private final int size;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public Page(
            List<T> content,
            long totalElements,
            int page,
            int size
    ) {
        this.content = content != null ? List.copyOf(content) : Collections.emptyList();
        this.totalElements = Math.max(0, totalElements);
        this.page = Math.max(0, page);
        this.size = size > 0 ? size : 10;

        this.totalPages = this.size > 0
                ? (int) Math.ceil((double) this.totalElements / this.size)
                : 0;

        this.first = this.page == 0;
        this.last = this.totalPages == 0 || this.page >= this.totalPages - 1;
    }

    public static <T> Page<T> of(List<T> content, long totalElements, int page, int size) {
        return new Page<>(content, totalElements, page, size);
    }

    public static <T> Page<T> empty(int page, int size) {
        return new Page<>(Collections.emptyList(), 0, page, size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        List<R> converted = this.content.stream().map(mapper).toList();
        return new Page<>(converted, this.totalElements, this.page, this.size);
    }

    public ResponseHTTP.PageMeta toMeta() {
        return new ResponseHTTP.PageMeta(
                this.page,
                this.size,
                this.totalElements,
                this.totalPages,
                hasNext(),
                hasPrevious()
        );
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}
