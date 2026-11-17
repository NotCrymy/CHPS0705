/**
 * Mini-jeu de démonstration (canvas) qui contient les 5 bugs du POC (Specs.md) :
 *   BUG1 traverse les murs · BUG2 perte d'une vie · BUG3 score doublé 30 s
 *   BUG4 commandes inversées 10 s · BUG5 texture manquante
 * Indépendant de Kapture : il ne parle à l'overlay que via le SDK (voir DemoGame.vue).
 */

export const DUREES: Record<string, number> = { BUG1: 8000, BUG2: 0, BUG3: 30000, BUG4: 10000, BUG5: 15000 };
export const BUGS = Object.keys(DUREES);

const W = 960;
const H = 540;
const VITESSE = 230;

interface Rect { x: number; y: number; w: number; h: number }
interface Drone { x: number; y: number; vx: number; vy: number }
interface Particule { x: number; y: number; vx: number; vy: number; vie: number; couleur: string }
interface Texte { x: number; y: number; t: string; vie: number; couleur: string }

export interface EtatJeu {
  niveau: number;
  score: number;
  vies: number;
  position: { x: number; y: number };
  effets: string[];
}

export interface Rappels {
  /** Un bug spontané vient d'apparaître (à relayer à Kapture). */
  bugSpontane: (titre: string) => void;
  /** HUD à rafraîchir. */
  change: () => void;
}

const MURS: Rect[] = [
  { x: 0, y: 0, w: W, h: 14 }, { x: 0, y: H - 14, w: W, h: 14 },
  { x: 0, y: 0, w: 14, h: H }, { x: W - 14, y: 0, w: 14, h: H },
  { x: 200, y: 110, w: 22, h: 200 }, { x: 420, y: 330, w: 260, h: 22 },
  { x: 700, y: 90, w: 22, h: 180 }, { x: 300, y: 430, w: 22, h: 96 },
  { x: 520, y: 130, w: 120, h: 22 },
];

const aleatoire = (min: number, max: number) => min + Math.random() * (max - min);

export class Jeu {
  joueur = { x: 110, y: 270, r: 12 };
  score = 0;
  vies = 3;
  niveau = 1;
  pause = false;
  spontanes = true;
  effets = new Map<string, number>();          // titre → fin (ms, performance.now)

  private ctx: CanvasRenderingContext2D;
  private touches = new Set<string>();
  private orbes: { x: number; y: number }[] = [];
  private drones: Drone[] = [];
  private particules: Particule[] = [];
  private textes: Texte[] = [];
  private traine: { x: number; y: number }[] = [];
  private invulnerableJusqua = 0;
  private secousse = 0;
  private prochainBug = performance.now() + aleatoire(15000, 30000);
  private dernier = performance.now();
  private raf = 0;

  constructor(private canvas: HTMLCanvasElement, private rappels: Rappels) {
    this.ctx = canvas.getContext("2d")!;
    for (let i = 0; i < 6; i++) this.ajouterOrbe();
    this.reinitialiserDrones();
    window.addEventListener("keydown", this.bas);
    window.addEventListener("keyup", this.haut);
    window.addEventListener("blur", this.relacher);
    this.raf = requestAnimationFrame(this.boucle);
  }

  detruire() {
    cancelAnimationFrame(this.raf);
    window.removeEventListener("keydown", this.bas);
    window.removeEventListener("keyup", this.haut);
    window.removeEventListener("blur", this.relacher);
  }

  etat(): EtatJeu {
    return {
      niveau: this.niveau,
      score: this.score,
      vies: this.vies,
      position: { x: Math.round(this.joueur.x), y: Math.round(this.joueur.y) },
      effets: [...this.effets.keys()],
    };
  }

  actif(titre: string) {
    return (this.effets.get(titre) ?? 0) > performance.now();
  }

  restant(titre: string) {
    return Math.max(0, (this.effets.get(titre) ?? 0) - performance.now());
  }

  /** Déclenche un bug du POC. Renvoie false si le titre est inconnu. */
  declencher(titre: string): boolean {
    if (!(titre in DUREES)) return false;
    const t = performance.now();
    if (titre === "BUG2") {
      this.perdreVie(true);
    } else {
      this.effets.set(titre, t + DUREES[titre]);
      const libelles: Record<string, string> = { BUG1: "Fantôme !", BUG3: "Score ×2", BUG4: "Commandes inversées", BUG5: "" };
      if (libelles[titre]) this.texte(this.joueur.x, this.joueur.y - 26, libelles[titre], "#e8e8e5");
    }
    this.rappels.change();
    return true;
  }

  /* ── Entrées ── */
  private bas = (e: KeyboardEvent) => {
    if ((e.target as HTMLElement)?.matches?.("input, select, textarea")) return;
    const k = e.key.toLowerCase();
    if (["arrowup", "arrowdown", "arrowleft", "arrowright", "z", "q", "s", "d", "w", "a"].includes(k)) {
      e.preventDefault();
      this.touches.add(k);
    }
  };
  private haut = (e: KeyboardEvent) => this.touches.delete(e.key.toLowerCase());
  private relacher = () => this.touches.clear();

  /* ── Boucle ── */
  private boucle = (t: number) => {
    const dt = Math.min(0.05, (t - this.dernier) / 1000);
    this.dernier = t;
    if (!this.pause) this.maj(dt, t);
    this.dessiner(t);
    this.raf = requestAnimationFrame(this.boucle);
  };

  private maj(dt: number, t: number) {
    // Effets expirés
    for (const [titre, fin] of this.effets) if (fin <= t) { this.effets.delete(titre); this.rappels.change(); }

    // Bugs spontanés
    if (this.spontanes && t >= this.prochainBug) {
      this.prochainBug = t + aleatoire(18000, 40000);
      const titre = BUGS[Math.floor(Math.random() * BUGS.length)];
      this.declencher(titre);
      this.rappels.bugSpontane(titre);
    }

    // Déplacement
    let dx = 0, dy = 0;
    const k = this.touches;
    if (k.has("arrowleft") || k.has("q") || k.has("a")) dx -= 1;
    if (k.has("arrowright") || k.has("d")) dx += 1;
    if (k.has("arrowup") || k.has("z") || k.has("w")) dy -= 1;
    if (k.has("arrowdown") || k.has("s")) dy += 1;
    if (this.actif("BUG4")) { dx = -dx; dy = -dy; }
    const n = Math.hypot(dx, dy) || 1;
    const fantome = this.actif("BUG1");
    this.deplacer((dx / n) * VITESSE * dt, 0, fantome);
    this.deplacer(0, (dy / n) * VITESSE * dt, fantome);
    if (dx || dy) {
      this.traine.push({ x: this.joueur.x, y: this.joueur.y });
      if (this.traine.length > 14) this.traine.shift();
    } else if (this.traine.length) this.traine.shift();

    // Orbes
    this.orbes = this.orbes.filter((o) => {
      if (Math.hypot(o.x - this.joueur.x, o.y - this.joueur.y) > this.joueur.r + 9) return true;
      const gain = this.actif("BUG3") ? 20 : 10;
      this.score += gain;
      this.texte(o.x, o.y - 12, `+${gain}`, this.actif("BUG3") ? "#e5c07b" : "#a9c9d6");
      this.eclat(o.x, o.y, "#a9c9d6", 14);
      const niveau = 1 + Math.floor(this.score / 150);
      if (niveau !== this.niveau) {
        this.niveau = niveau;
        this.texte(W / 2, H / 2, `Niveau ${niveau}`, "#e8e8e5");
        this.reinitialiserDrones();
      }
      this.rappels.change();
      return false;
    });
    while (this.orbes.length < 6) this.ajouterOrbe();

    // Drones
    for (const d of this.drones) {
      d.x += d.vx * dt;
      d.y += d.vy * dt;
      if (d.x < 30 || d.x > W - 30) d.vx *= -1;
      if (d.y < 30 || d.y > H - 30) d.vy *= -1;
      if (t > this.invulnerableJusqua && Math.hypot(d.x - this.joueur.x, d.y - this.joueur.y) < this.joueur.r + 11) {
        this.perdreVie(false);
      }
    }

    // Particules et textes
    for (const p of this.particules) { p.x += p.vx * dt; p.y += p.vy * dt; p.vx *= 0.92; p.vy *= 0.92; p.vie -= dt * 1.6; }
    this.particules = this.particules.filter((p) => p.vie > 0);
    for (const tx of this.textes) { tx.y -= 30 * dt; tx.vie -= dt * 0.9; }
    this.textes = this.textes.filter((tx) => tx.vie > 0);
    this.secousse = Math.max(0, this.secousse - dt * 30);
  }

  private deplacer(dx: number, dy: number, fantome: boolean) {
    const j = this.joueur;
    const nx = Math.min(W - j.r, Math.max(j.r, j.x + dx));
    const ny = Math.min(H - j.r, Math.max(j.r, j.y + dy));
    if (!fantome && MURS.some((m) => nx + j.r > m.x && nx - j.r < m.x + m.w && ny + j.r > m.y && ny - j.r < m.y + m.h)) return;
    j.x = nx;
    j.y = ny;
  }

  private perdreVie(sansRaison: boolean) {
    this.vies -= 1;
    this.secousse = 12;
    this.invulnerableJusqua = performance.now() + 1500;
    this.eclat(this.joueur.x, this.joueur.y, "#c46a5e", 22);
    this.texte(this.joueur.x, this.joueur.y - 26, sansRaison ? "−1 vie ?!" : "−1 vie", "#e7a59b");
    if (this.vies <= 0) {
      this.texte(W / 2, H / 2, "Partie terminée", "#e7a59b");
      this.vies = 3;
      this.score = 0;
      this.niveau = 1;
      this.reinitialiserDrones();
    }
    this.rappels.change();
  }

  private ajouterOrbe() {
    for (let essai = 0; essai < 50; essai++) {
      const o = { x: aleatoire(40, W - 40), y: aleatoire(40, H - 40) };
      if (!MURS.some((m) => o.x > m.x - 16 && o.x < m.x + m.w + 16 && o.y > m.y - 16 && o.y < m.y + m.h + 16)) {
        this.orbes.push(o);
        return;
      }
    }
  }

  private reinitialiserDrones() {
    const nb = 2 + this.niveau;
    this.drones = Array.from({ length: Math.min(nb, 7) }, () => {
      const angle = aleatoire(0, Math.PI * 2);
      const v = 90 + this.niveau * 15;
      return { x: aleatoire(W * 0.45, W - 60), y: aleatoire(60, H - 60), vx: Math.cos(angle) * v, vy: Math.sin(angle) * v };
    });
  }

  private eclat(x: number, y: number, couleur: string, nb: number) {
    for (let i = 0; i < nb; i++) {
      const a = aleatoire(0, Math.PI * 2);
      const v = aleatoire(60, 220);
      this.particules.push({ x, y, vx: Math.cos(a) * v, vy: Math.sin(a) * v, vie: 1, couleur });
    }
  }

  private texte(x: number, y: number, t: string, couleur: string) {
    this.textes.push({ x, y, t, vie: 1, couleur });
  }

  /* ── Rendu ── */
  private dessiner(t: number) {
    const c = this.canvas;
    const dpr = window.devicePixelRatio || 1;
    const largeur = c.clientWidth, hauteur = c.clientHeight;
    if (c.width !== Math.round(largeur * dpr) || c.height !== Math.round(hauteur * dpr)) {
      c.width = Math.round(largeur * dpr);
      c.height = Math.round(hauteur * dpr);
    }
    const ctx = this.ctx;
    const echelle = Math.min(c.width / W, c.height / H);
    const ox = (c.width - W * echelle) / 2, oy = (c.height - H * echelle) / 2;
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.fillStyle = "#1f2023";
    ctx.fillRect(0, 0, c.width, c.height);
    const sx = this.secousse ? aleatoire(-this.secousse, this.secousse) : 0;
    const sy = this.secousse ? aleatoire(-this.secousse, this.secousse) : 0;
    ctx.setTransform(echelle, 0, 0, echelle, ox + sx * echelle, oy + sy * echelle);

    // Sol
    if (this.actif("BUG5")) {
      const pas = 24;
      for (let y = 0; y < H; y += pas) for (let x = 0; x < W; x += pas) {
        ctx.fillStyle = ((x + y) / pas) % 2 ? "#000" : "#ff00dc";
        ctx.fillRect(x, y, pas, pas);
      }
    } else {
      ctx.fillStyle = "#2b2d31";
      ctx.fillRect(0, 0, W, H);
      ctx.strokeStyle = "rgb(255 255 255 / 0.04)";
      ctx.lineWidth = 1;
      for (let x = 0; x <= W; x += 40) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }
      for (let y = 0; y <= H; y += 40) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); }
    }

    // Murs
    // Rendu mat : aucun halo (shadowBlur) sur les formes.
    ctx.shadowBlur = 0;
    ctx.fillStyle = this.actif("BUG1") ? "rgb(68 71 78 / 0.4)" : "#44474e";
    for (const m of MURS) ctx.fillRect(m.x, m.y, m.w, m.h);

    // Orbes
    for (const o of this.orbes) {
      const p = 1 + Math.sin(t / 220 + o.x) * 0.15;
      ctx.fillStyle = "#a9c9d6";
      ctx.beginPath(); ctx.arc(o.x, o.y, 6 * p, 0, Math.PI * 2); ctx.fill();
    }

    // Drones
    for (const d of this.drones) {
      ctx.save();
      ctx.translate(d.x, d.y);
      ctx.rotate(t / 300);
      ctx.fillStyle = "#b5534b";
      ctx.fillRect(-9, -9, 18, 18);
      ctx.restore();
    }

    // Traîne + joueur
    const fantome = this.actif("BUG1");
    this.traine.forEach((p, i) => {
      ctx.fillStyle = `rgb(229 192 123 / ${(i / this.traine.length) * 0.25})`;
      ctx.beginPath(); ctx.arc(p.x, p.y, this.joueur.r * (i / this.traine.length), 0, Math.PI * 2); ctx.fill();
    });
    const clignote = t < this.invulnerableJusqua && Math.floor(t / 100) % 2 === 0;
    if (!clignote) {
      ctx.globalAlpha = fantome ? 0.45 : 1;
      ctx.fillStyle = this.actif("BUG4") ? "#c46a5e" : "#e5c07b";
      ctx.beginPath(); ctx.arc(this.joueur.x, this.joueur.y, this.joueur.r, 0, Math.PI * 2); ctx.fill();
      ctx.globalAlpha = 1;
    }

    // Particules et textes
    for (const p of this.particules) {
      ctx.globalAlpha = Math.max(0, p.vie);
      ctx.fillStyle = p.couleur;
      ctx.fillRect(p.x - 2, p.y - 2, 4, 4);
    }
    ctx.textAlign = "center";
    ctx.font = "600 15px Inter, system-ui, sans-serif";
    for (const tx of this.textes) {
      ctx.globalAlpha = Math.max(0, tx.vie);
      ctx.fillStyle = tx.couleur;
      ctx.fillText(tx.t, tx.x, tx.y);
    }
    ctx.globalAlpha = 1;

    if (this.pause) {
      ctx.fillStyle = "rgb(18 19 22 / 0.5)";
      ctx.fillRect(0, 0, W, H);
      ctx.fillStyle = "#e8e8e5";
      ctx.font = "600 22px Inter, system-ui, sans-serif";
      ctx.fillText("Pause", W / 2, H / 2);
    }
  }
}
