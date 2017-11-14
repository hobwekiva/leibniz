package leibniz
package variance

import leibniz.internal.Unsafe

sealed abstract class Injective[F[_]] { F =>
  import Injective._

    /**
      * Every injection f with non-empty domain has a left inverse g (in conventional mathematics).
      */
    type Retraction[X <: F[_]]
    def retraction[A]: Retraction[F[A]] === A = {
      // Doesn't seem possible to prove unless F has a type member for its
      // type parameter.
      Is.force(Unsafe.unsafe)
    }

  /**
    * F is injective if and only if, given any G, H whenever F ∘ G = F ∘ H, then G = H.
    * In other words, injective type constructors are precisely the monomorphisms in the category
    * of types and type constructors.
    */
  def monomorphism[G[_], H[_]](p: λ[x => F[G[x]]] =~= λ[x => F[H[x]]]): G =~= H = {
    // Using proof.
    {
      Axioms.tcExtensionality[G, H].applyT { t =>
        type T = t.Type
        F.proof[G[T], H[T]](p.lower[λ[x[_] => x[T]]])
      }
    }
  }

  /**
    * The function f is said to be injective provided that for all a and b in X,
    * whenever f(a) = f(b), then a = b.
    */
  def proof[A, B](ev: F[A] === F[B]): A === B = {
    // Using monomorphism.
    {
      val p: λ[x => F[A]] =~= λ[x => F[B]] = IsK.const[F[A], F[B]](ev)
      val q: λ[x => A] =~= λ[x => B] = monomorphism[λ[x => A], λ[x => B]](p)
      q.lower[λ[f[_] => f[Unit]]]
    }

    // Using retraction.
    {
      val p = Leibniz.fromIs[Nothing, F[_], F[A], F[B]](ev)
      retraction[A].flip andThen p.lift[Nothing, Any, Retraction].toIs andThen retraction[B]
    }
  }

  /**
    * If A ≠ B, then F[A] ≠ F[B].
    */
  def contrapositive[A, B](ev: A =!= B): F[A] =!= F[B] =
    WeakApart.witness[F[A], F[B]] { fab =>
      ev.contradicts(proof(fab))
    }

  /**
    * Constant type constructors are not injective.
    */
  def contradicts(constant: Constant[F]): Void =
    F.proof(constant.proof[Unit, Void]).coerce(())

  /**
    * If G ∘ F is injective, then F is injective (but G need not be).
    */
  def decompose[G[_], H[_]](implicit p: F =~= λ[x => G[H[x]]]): Injective[H] = {
    val GH: Injective[λ[x => G[H[x]]]] = p.subst[Injective](F)

    new Injective[H] {
      override def monomorphism[I[_], J[_]](p: λ[x => H[I[x]]] =~= λ[x => H[J[x]]]): I =~= J = {
        type f[x[_], a] = G[x[a]]
        val q : λ[x => G[H[I[x]]]] =~= λ[x => G[H[J[x]]]] = p.lift[f]
        GH.monomorphism[I, J](q)
      }

      override def proof[A, B](ev: H[A] === H[B]): A === B =
        GH.proof(ev.lift[G])
    }
  }

  /**
    * If F is injective and A and B are both types, then F[A ∩ B] = F[A] ∩ F[B].
    */
  def intersection[A, B]: F[A with B] === (F[A] with F[B]) =
    Axioms.tcIntersection[F, A, B]

  /**
    * If F and G are both injective, then F ∘ G is injective.
    */
  def compose[G[_]](implicit G: Injective[G]): Injective[λ[x => F[G[x]]]] =
    new Compose[F, G](F, G)

  /**
    * If F and G are both injective, then G ∘ F is injective.
    */
  def andThen[G[_]](implicit G: Injective[G]): Injective[λ[x => G[F[x]]]] =
    new Compose[G, F](G, F)
}
object Injective {
  def apply[F[_]](implicit ev: Injective[F]): Injective[F] = ev

//  def proveViaRetraction[F[_], R[_ <: F[_]]](p: λ[x => R[F[x]]] =~= λ[x => x]): Injective[F] = new Injective[F] {
//    override def monomorphism[G[_], H[_]](q: λ[x => F[G[x]]] =~= λ[x => F[H[x]]]): G =~= H = {
//
//    }
//  }

//  final class FromRetraction[F[_], R[_ <: F[_]]] extends Injective[F] {
//    type Retraction[X <: F[_]]
//    def retraction[A]: Retraction[F[A]] === A = Is.force[Retraction[F[A]], A]
//  }

//  class Foo[A] { final type Type = A }
//
//  final case class FooInj() extends Injective[Foo] {
//    final type Retraction[X <: Foo[_]] = X#Type
//    implicitly[Retraction[Foo[Int]] <:< Int]
//    override def retraction[A]: Retraction[Foo[A]] === A = Is.refl[A]
//  }

  final case class Id() extends Injective[λ[X => X]] {
    override def monomorphism[G[_], H[_]](p: λ[x => G[x]] =~= λ[x => H[x]]): =~=[G, H] = p
  }

  final case class Compose[F[_], G[_]](F: Injective[F], G: Injective[G]) extends Injective[λ[x => F[G[x]]]] {
    override def proof[A, B](ev: F[G[A]] === F[G[B]]): A === B =
      G.proof[A, B](F.proof[G[A], G[B]](ev))
  }

  /**
    * `unsafeForce` abuses `asInstanceOf` to explicitly coerce types.
    * It is unsafe, but necessary in most cases.
    */
  def force[F[_]](implicit unsafe: Unsafe): Injective[F] = {
    unsafe.coerceK2_2[Injective, F].apply[λ[X => X]](Id())
  }
}