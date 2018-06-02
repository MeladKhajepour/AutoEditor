package com.example.android.autoeditor.floatingToolbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.view.menu.MenuBuilder;
import android.view.Menu;

@SuppressWarnings("RestrictedApi")
public class FloatingToolbarMenuBuilder {

    private MenuBuilder menuBuilder;

    public FloatingToolbarMenuBuilder(Context context) {
        menuBuilder = new MenuBuilder(context);
    }

    public FloatingToolbarMenuBuilder addItem(int id, Drawable icon, String title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(int id, Drawable icon, @StringRes int title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(int id, @DrawableRes int icon,
                                              @StringRes int title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(int id, @DrawableRes int icon, String title) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, title).setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(int id, @DrawableRes int icon) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, "").setIcon(icon);
        return this;
    }

    public FloatingToolbarMenuBuilder addItem(int id, Drawable icon) {
        menuBuilder.add(Menu.NONE, id, Menu.NONE, "").setIcon(icon);
        return this;
    }

    public Menu build() {
        return menuBuilder;
    }

}