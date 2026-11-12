package hmcnav;

import net.minecraft.util.MovementInput;

/** Wraps the player's movement input; while navigating, the Navigator drives it, otherwise the original input is used. */
public class NavMovementInput extends MovementInput {
    public final MovementInput original;

    public NavMovementInput(MovementInput original) { this.original = original; }

    @Override
    public void func_78898_a() {
        Navigator nav = Navigator.INSTANCE;
        if (nav.isActive()) {
            field_78902_a = 0f; field_78900_b = 0f; field_78901_c = false; field_78899_d = false;
            try { nav.tick(this); } catch (Throwable t) { nav.fail("error: " + t); }
        } else if (original != null) {
            original.func_78898_a();
            field_78902_a = original.field_78902_a;
            field_78900_b = original.field_78900_b;
            field_78901_c = original.field_78901_c;
            field_78899_d = original.field_78899_d;
        }
        if (nav.jumpActive()) field_78901_c = true;   // holding jump = jump again on every landing
        else nav.maybeRestore();
    }
}
