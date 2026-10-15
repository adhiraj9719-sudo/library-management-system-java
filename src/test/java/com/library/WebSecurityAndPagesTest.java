package com.library;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.library.dto.BookForm;
import com.library.dto.MemberForm;
import com.library.model.Book;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebSecurityAndPagesTest {

    @Autowired MockMvc mvc;
    @Autowired BookService books;
    @Autowired MemberService members;

    private static final String ADMIN = "admin@library.local";

    private Book addBook() {
        BookForm f = new BookForm();
        f.setTitle("Dune");
        f.setAuthor("Frank Herbert");
        f.setIsbn("9780441172719");
        f.setGenre("Science Fiction");
        f.setTotalCopies("2");
        return books.create(f);
    }

    private User addMember() {
        MemberForm f = new MemberForm();
        f.setName("Ann");
        f.setEmail("ann@example.com");
        f.setPassword("password123");
        return members.create(f);
    }

    @Test
    void anonymousUsersAreSentToLogin() throws Exception {
        mvc.perform(get("/librarian/books")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/member")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void librarianCanSignIn() throws Exception {
        mvc.perform(formLogin("/login").user("email", ADMIN).password("password", "admin12345"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/librarian"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin("/login").user("email", ADMIN).password("password", "nope"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void librarianPagesRender() throws Exception {
        Book b = addBook();
        addMember();
        for (String url : new String[] {"/librarian", "/librarian/books", "/librarian/books/new",
                "/librarian/books/" + b.getId() + "/edit", "/librarian/members", "/librarian/members/new",
                "/librarian/transactions", "/librarian/notifications", "/librarian/reports",
                "/librarian/reports/data", "/librarian/reports/inventory.csv"}) {
            mvc.perform(get(url).with(user(ADMIN).roles("LIBRARIAN"))).andExpect(status().isOk());
        }
    }

    @Test
    void membersCannotOpenLibrarianPages() throws Exception {
        addMember();
        mvc.perform(get("/librarian/books").with(user("ann@example.com").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void postsWithoutCsrfTokenAreRejected() throws Exception {
        mvc.perform(post("/librarian/books/new").with(user(ADMIN).roles("LIBRARIAN"))
                .param("title", "X")).andExpect(status().isForbidden());
    }

    @Test
    void librarianCanAddABookThroughTheForm() throws Exception {
        mvc.perform(post("/librarian/books/new").with(user(ADMIN).roles("LIBRARIAN")).with(csrf())
                        .param("title", "Neuromancer").param("author", "William Gibson")
                        .param("isbn", "9780441569595").param("genre", "Science Fiction")
                        .param("totalCopies", "2"))
                .andExpect(redirectedUrl("/librarian/books"));
        mvc.perform(get("/librarian/books").with(user(ADMIN).roles("LIBRARIAN")))
                .andExpect(content().string(containsString("Neuromancer")));
    }

    @Test
    void invalidBookShowsAnErrorAndKeepsTheForm() throws Exception {
        mvc.perform(post("/librarian/books/new").with(user(ADMIN).roles("LIBRARIAN")).with(csrf())
                        .param("title", "Bad").param("author", "A").param("isbn", "12")
                        .param("genre", "G").param("totalCopies", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("valid ISBN")));
    }

    @Test
    void memberBorrowAndHistoryFlow() throws Exception {
        Book b = addBook();
        addMember();
        var ann = user("ann@example.com").roles("MEMBER");

        mvc.perform(get("/member").with(ann)).andExpect(status().isOk());
        mvc.perform(get("/member/search").param("q", "dune").with(ann))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dune")));
        mvc.perform(post("/member/borrow/" + b.getId()).with(ann).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/member/history").with(ann))
                .andExpect(content().string(containsString("Dune")));
        mvc.perform(get("/member/notifications").with(ann)).andExpect(status().isOk());
        mvc.perform(get("/member/profile").with(ann)).andExpect(status().isOk());
        mvc.perform(get("/member/api/notifications").with(ann))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").exists());
    }
}
