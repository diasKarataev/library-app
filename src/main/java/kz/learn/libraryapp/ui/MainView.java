package kz.learn.libraryapp.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    public MainView() {
        H1 header = new H1("Library App");

        Anchor booksLink = new Anchor("books", "Books");

        Anchor authorsLink = new Anchor("authors", "Authors");

        add(header, booksLink, authorsLink);

        setAlignItems(Alignment.CENTER);
    }
}
