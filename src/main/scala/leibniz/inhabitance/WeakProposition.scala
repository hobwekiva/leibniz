package leibniz
package inhabitance

import leibniz.internal.Unsafe

trait WeakProposition[A] {
  def equal[X : InhabitedSubset[?, A], Y: InhabitedSubset[?, A]]: X === Y =
    Unsafe.is[X, Y]

  def contractible(implicit A: ¬¬[A]): Contractible[A] =
    Contractible.witness[A](A, this)
}
object WeakProposition {
  def apply[A](implicit A: WeakProposition[A]): WeakProposition[A] = A

  // All values are equal.
  implicit def eq[A](implicit prop: WeakProposition[A]): Eq[A] =
    Eq.propositionEq[A]

  // Proposition
  implicit def prop[A](implicit prop: WeakProposition[A]): Proposition[WeakProposition[A]] =
    (_: Inhabited[WeakProposition[A]]) => prop
}
